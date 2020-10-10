layui.use(['element', 'layedit', 'form', 'vhttp', 'vtools'], function () {
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vtools = layui.vtools;

    function loadHistory(mform, data) {
        var history = localStorage.getItem("history");
        if (undefined == history) {
            history = [];
        } else {
            history = JSON.parse(localStorage.getItem("history"));
            if (history.length > 20) {
                history.shift();
            }
        }
        if (undefined != data) {
            history.push(data);
        }
        localStorage.setItem("history", JSON.stringify(history));
        var historyString = [];
        for (var i = history.length; i > 0; i--) {
            var his = history[i - 1];
            historyString.push('<option value="' + (i - 1) + '">' + his.time + " " + his.type + " " + his.url + '</option>');
        }
        $("#history").html(historyString.join(""));
        mform.render("select");
    }

    form.on('submit(search)', function (data) {
        var url = data.field.url;
        var type = data.field.method;
        var body = data.field.rqbody;
        loadHistory(form, { "url": url, "type": type, "body": body, "time": vtools.getTime() });
        $("#rspbody").html("...");
        vhttp.ajax(url, type, body, function (d) {
            if (typeof d == "object") {
                vtools.renderJson("rspbody", d);
            } else {
                vtools.renderHtml("rspbody", d, vtools.formatText);
            }
        }, function (d) {
            if (typeof d == "object") {
                if (undefined == d.responseJSON) {
                    vtools.renderHtml("rspbody", d.responseText, vtools.formatText);
                } else {
                    vtools.renderJson("rspbody", d.responseJSON);
                }
            } else {
                vtools.renderHtml("rspbody", d, vtools.formatText);
            }
        });
        return false;
    });
    form.on('select(history)', function (data) {
        var history = JSON.parse(localStorage.getItem("history"));
        form.val('restForm', {
            method: history[data.value].type
            , url: history[data.value].url
            , rqbody: history[data.value].body ? history[data.value].body : ""
        })
    });
    loadHistory(form);
    form.render();
});