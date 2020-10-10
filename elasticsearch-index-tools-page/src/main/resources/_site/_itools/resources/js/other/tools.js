layui.use(['element', 'layedit', 'form', 'vhttp', 'vtools'], function () {
    var vhttp = layui.vhttp;
    var vtools = layui.vtools;

    $("button.layui-btn").click(function () {
        $("#rspbody").html("...")
        vhttp.ajax($(this).attr("data"), "GET", function (d) {
            if (typeof d == "object") {
                vtools.renderJson("rspbody", d);
            } else {
                vtools.renderHtml("rspbody", d, vtools.formatText);
            }
        }, function (d) {
            if (typeof d == "object") {
                vtools.renderJson("rspbody", d.responseJSON);
            } else {
                vtools.renderHtml("rspbody", d, vtools.formatText);
            }
        })
    })
})