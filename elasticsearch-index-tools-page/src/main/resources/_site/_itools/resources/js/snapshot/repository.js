layui.use(['element', 'table', 'form', 'vhttp', 'vmodule', 'vtools', 'vtpl'], function () {
    var table = layui.table;
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vmodule = layui.vmodule;
    var vtools = layui.vtools;
    var vtpl = layui.vtpl;

    form.on('submit(create)', function (data) {
        var rdata = {};
        rdata.type = data.field.type;
        rdata.settings = {};
        layui.each(data.field, function (i, t) {
            if (i == "type") {
                rdata.type = t;
            } else if (i == "compress") {
                rdata.settings.compress = (t == "on");
            } else if (i == "repository") {
            } else if (i == "chunk_size") {
                //该值不能为空
                if ("" != t) {
                    rdata.settings[i] = t;
                }
            } else {
                rdata.settings[i] = t;
            }
        });
        console.log(JSON.stringify(rdata))
        vhttp.ajax("/_snapshot/" + data.field.repository, "PUT", JSON.stringify(rdata), function (data) {
            vmodule.showConfirm("创建成功", function (layer) {
                loadRepository();
                layer.closeAll();
            })
        })
        return false;
    });

    var renderSelect = function (stype) {
        if ("fs" == stype) {
            vtpl.render(settings, [
                { label: "存储路径", name: "location", ph: "文件保存路径" }
                , { label: "chunk size", name: "chunk_size", ph: "格式(1g,5m,100k),默认无限制", allowEmpty: true }
            ], "settingsForm");
        } else if ("hdfs" == stype) {
            vtpl.render(settings, [
                { label: "uri", name: "uri", ph: "hdfs://<host>:<port>/" }
                , { label: "存储路径", name: "path", ph: "文件保存路径" }
                , { label: "hadoop配置", name: "conf_location", ph: "hadoop配置文件路径(逗号分隔)" }
                , { label: "chunk size", name: "chunk_size", ph: "格式(1g,5m,100k),默认无限制", allowEmpty: true }
                , { label: "加载hadoop默认配置", name: "load_defaults", checkbox: true}
            ], "settingsForm");
        }
        form.render();
    }

    form.on('select(changeType)', function (data) {
        renderSelect(data.value);
    });

    renderSelect("fs");

    var repositoryTable = table.render({
        elem: '#repositoryTable'
        , height: window.screen.availHeight - 440
        , limit: 5000
        , page: false
        , cols: [[
            { field: 'repository', title: '仓库名称', width: 200 }
            , { field: 'type', title: '存储类型', width: 200 }
            , { field: 'location', title: '存储路径', width: 300 }
            , { field: 'compress', title: '是否压缩', width: 150 }
            , { field: 'mark', title: '备注' }
            , { fixed: 'right', width: 178, align: 'center', toolbar: '#repositoryTableBar' }
        ]]
        , data: []
    });

    table.on('tool(repository)', function (obj) {
        var data = obj.data;
        if (obj.event === 'del') {
            vmodule.rConfirm({
                msg: "确定删除: " + data.repository + " ?"
                , url: "/_snapshot/" + data.repository
                , method: "DELETE"
                , callback: function () {
                    loadRepository();
                    layer.closeAll();
                }
            })
        }
    });

    var loadRepository = function () {
        vhttp.ajax("/_snapshot/_all", "GET", function (data) {
            var records = [];
            for (var i in data) {
                records.push({
                    repository: i
                    , type: data[i].type
                    , location: data[i].settings.location
                    , compress: data[i].settings.compress == "true" ? "是" : "否"
                    , mark: vtools.toString(data[i].settings)
                });
            }
            repositoryTable.reload({
                data: records
            });
        })
    }

    loadRepository();
})