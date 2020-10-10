layui.use(['element', 'table', 'form', 'vhttp', 'vmodule'], function () {
    var table = layui.table;
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vmodule = layui.vmodule;

    vhttp.ajax("/_snapshot/_all", "GET", function (data) {
        var opts = [];
        for (var i in data) {
            opts.push('<option value="' + i + '">' + i + '</option>');
        }
        $("select[name='repository']").html(opts.join(""));
        form.render();
    })

    form.on('submit(create)', function (data) {
        vhttp.ajax("/_snapshot/" + data.field.repository + "/" + data.field.snapshot + "?wait_for_completion=" + (data.field.completion == "on")
            , "PUT"
            , JSON.stringify({
                "indices": data.field.indices.split(","),
                "ignore_unavailable": true,
                "include_global_state": false
            }), function () {
                loadBackup();
            })
        setTimeout(function () { loadBackup(); }, 1000)
        return false;
    })

    var repositoryBackupTable = table.render({
        elem: '#backupTable'
        , height: window.screen.availHeight - 440
        , limit: 5000
        , page: false
        , cols: [[
            { field: 'repository', title: '仓库名称', width: 200 }
            , { field: 'snapshot', title: '快照名称', width: 200 }
            , { field: 'version', title: '版本', width: 100 }
            , { field: 'state', title: '状态', width: 150 }
            , { field: 'indexCount', title: '索引个数', width: 100 }
            , { field: 'indexList', title: '索引' }
            , { field: 'endTime', title: '生成时间', width: 220 }
            , { fixed: 'right', width: 178, align: 'center', toolbar: '#backupTableBar' }
        ]]
        , data: []
    });

    table.on('tool(backup)', function (obj) {
        var data = obj.data;
        if (obj.event === 'del') {
            vmodule.rConfirm({
                msg: "确定删除: " + data.repository + ":" + data.snapshot + " ?"
                , url: "/_snapshot/" + data.repository + "/" + data.snapshot
                , method: "DELETE"
                , callback: function () {
                    loadBackup();
                    layer.closeAll();
                }
            })
        } else if (obj.event === 'restore') {
            vmodule.rConfirm({
                msg: "确定恢复: " + data.repository + ":" + data.snapshot + " ?"
                , url: "/_snapshot/" + data.repository + "/" + data.snapshot + "/_restore"
                , method: "POST"
                , callback: function () {
                    loadBackup();
                    layer.closeAll();
                }
            })
        }
    });

    var loadTable = function (repositories, index, records) {
        if (index >= repositories.length) {
            repositoryBackupTable.reload({
                data: records
            });
            return;
        }
        var name = repositories[index];
        vhttp.ajax("/_snapshot/" + name + "/_all", "GET", function (data) {
            for (var i in data.snapshots) {
                var sn = data.snapshots[i];
                records.push({
                    repository: name
                    , snapshot: sn.snapshot
                    , version: sn.version
                    , state: sn.state
                    , indexCount: sn.indices.length
                    , indexList: sn.indices.join(",")
                    , endTime: sn.end_time
                });
            }
            loadTable(repositories, ++index, records);
        })
    }

    var loadBackup = function () {
        vhttp.ajax("/_snapshot/_all", "GET", function (data) {
            var repositories = [];
            for (var i in data) {
                repositories.push(i);
            }
            loadTable(repositories, 0, []);
        })
    }

    loadBackup();
})