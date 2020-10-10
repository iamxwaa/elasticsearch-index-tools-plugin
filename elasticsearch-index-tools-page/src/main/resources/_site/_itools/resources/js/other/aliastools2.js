layui.use(['element', 'layedit', 'form', 'vhttp', 'vtools'], function () {
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vtools = layui.vtools;

    form.on('submit(submitAlias)', function (data) {
        var indexName = data.field.indexName;
        var indexNameFormat = data.field.indexNameFormat;
        var indexExist = data.field.indexExist;
        var indexFormat = data.field.indexFormat;
        var aliasFormat = data.field.aliasFormat;
        var timeField = data.field.timeField;
        var startEndDate = data.field.startEndDate;
        $("#rspbody2").html("别名创建中...");
        vhttp.ajax("/_itools/index/aliascreate", "PUT", JSON.stringify({
            "indexName": indexName
            , "indexNameFormat": indexNameFormat
            , "indexExist": indexExist
            , "indexFormat": indexFormat
            , "aliasFormat": aliasFormat
            , "timeField": timeField
            , "startEndDate": startEndDate
        }), function (d) {
            vhttp.ajax(indexName + "*/_alias/", "GET", function (d2) {
                if (typeof d2 == "object") {
                    vtools.renderJson("rspbody2", d2)
                } else {
                    vtools.renderHtml("rspbody2", d2, vtools.formatText)
                }
            })
        }, function (d) {
            if (typeof d == "object") {
                if (undefined == d.responseJSON) {
                    vtools.renderHtml("rspbody2", d.responseText, vtools.formatText)
                } else {
                    vtools.renderJson("rspbody2", d.responseJSON)
                }
            } else {
                vtools.renderHtml("rspbody2", d, vtools.formatText)
            }
        });
        return false;
    });
    form.render();
});