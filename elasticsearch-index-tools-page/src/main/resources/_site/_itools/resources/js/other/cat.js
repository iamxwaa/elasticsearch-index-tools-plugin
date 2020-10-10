layui.use(['form', 'table', 'vhttp'], function () {
    var form = layui.form;
    var table = layui.table;
    var vhttp = layui.vhttp;

    function renderTable(data) {
        var lines = data.split("\n");

        var colunmRange = [];
        var fieldArr = [];
        for (var i in lines[0]) {
            var current = lines[0][i];
            var next = lines[0][parseInt(i) + 1];
            fieldArr.push(current);
            if (i == lines[0].length - 1) {
                colunmRange.push(fieldArr.join(""));
                break;
            }
            if (current == ' ' && next != ' ') {
                colunmRange.push(fieldArr.join(""));
                fieldArr = [];
                continue;
            }
        }
        var cols = [];
        for (var i = 0; i < colunmRange.length; i++) {
            var name = colunmRange[i];
            if (i > 0) {
                var s1 = cols[i - 1].end;
                var s2 = s1 + colunmRange[i].length;
                cols.push({ title: name.trim(), field: name.trim(), start: s1, end: s2 });
            } else {
                cols.push({ title: name.trim(), field: name.trim(), start: 0, end: colunmRange[i].length });
            }
        }
        var datas = [];
        for (var i = 1; i < lines.length; i++) {
            if ("" == lines[i]) {
                continue;
            }
            var d = {};
            for (var j = 0; j < cols.length; j++) {
                if (j == cols.length - 1) {
                    d[cols[j].field] = lines[i].substring(cols[j].start);
                } else {
                    d[cols[j].field] = lines[i].substring(cols[j].start, cols[j].end);
                }
            }
            datas.push(d);
        }

        table.render({
            elem: '#rspTable'
            , height: window.screen.availHeight - 380
            , limit: 5000
            , page: false
            , cols: [cols]
            , data: datas
            , toolbar: true
        });
    }

    form.on('submit(doAction)', function (data) {
        vhttp.ajax(data.field.action + "?v", "GET", function (d) {
            renderTable(d)
        })
        return false;
    });
    form.render();
});