layui.use(['form', 'layer', 'table', 'vhttp', 'vtools', 'vmodule'], function () {
    var form = layui.form;
    var layer = layui.layer;
    var table = layui.table;
    var vhttp = layui.vhttp;
    var vtools = layui.vtools;
    var vmodule = layui.vmodule;

    var _autoCreateFormHtml = `
<form class="layui-form layui-form-pane" action="" lay-filter="autoCreateForm" style="padding: 10px;">
    <div class="layui-form-item">
        <label class="layui-form-label">ID</label>
            <div class="layui-input-block">
            <input type="text" name="id" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">索引名称</label>
            <div class="layui-input-block">
            <input type="text" name="name" lay-verify="required" placeholder="请输入内容" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">索引名称格式</label>
            <div class="layui-input-block">
            <input type="text" name="nameFormat" value="<NAME>-<TIME>" lay-verify="required" placeholder="请输入内容" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">索引时间格式</label>
            <div class="layui-input-block">
            <input type="text" name="format" value="yyyy.MM" lay-verify="required" placeholder="请输入内容" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">别名时间格式</label>
            <div class="layui-input-block">
            <input type="text" name="aliasFormat" value="yyyy.MM.dd" lay-verify="required" placeholder="请输入内容" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">时间字段</label>
            <div class="layui-input-block">
            <input type="text" name="timeField" value="time" lay-verify="required" placeholder="请输入内容" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <div class="layui-input-block" style="margin-left: 510px;">
            <button type="submit" class="layui-btn" lay-submit="" lay-filter="autoCreateFormSubmit">提交</button>
            <button type="button" class="layui-btn layui-btn-primary" id="autoCreateFormClose">关闭</button>
        </div>
    </div>
</form>`;

    var autoCreateTable = table.render({
        elem: '#autoCreateTable'
        , toolbar: '#autoCreateToolBar'
        , defaultToolbar: []
        , height: window.screen.availHeight - 300
        , page: true
        , url: "/_itools/autocreate"
        , cols: [[
            { field: 'id', title: 'ID', width: 250 }
            , { field: 'name', title: '索引名称', width: 200, sort: true }
            , { field: 'nameFormat2', title: '索引名称格式', width: 200 }
            , { field: 'format', title: '时间格式', width: 160 }
            , { field: 'aliasFormat', title: '别名时间格式', width: 160 }
            , { field: 'timeField', title: '时间字段', width: 100 }
            , { align: 'center', toolbar: '#autoCreateTableBar' }
        ]]
        , parseData: function (res) {
            for (var i in res.data) {
                res.data[i].nameFormat2 = vtools.toString(res.data[i].nameFormat);
            }
            return res;
        }
    });

    table.on('tool(autoCreate)', function (obj) {
        var data = obj.data;
        if (obj.event === 'detail') {
            var t = []
            t.push("索引名称:" + data.name);
            t.push("索引名称格式:" + vtools.toString(data.nameFormat));
            t.push("时间格式:" + data.format);
            t.push("别名时间格式:" + data.aliasFormat);
            t.push("时间字段:" + data.timeField);
            layer.msg(t.join("<br>"));
        } else if (obj.event === 'del') {
            vmodule.rConfirm({
                msg: "确定删除: " + data.name + " ?"
                , url: "/.index-tools-auto-create/doc/" + data.id
                , method: "DELETE"
                , callback: function () {
                    vhttp.ajax("/.index-tools-auto-create/_refresh", "GET", function () {
                        autoCreateTable.reload();
                        layer.closeAll();
                    })
                }
            })
        } else if (obj.event === 'edit') {
            layer.open({
                type: 1
                , area: '800px'
                , offset: 't'
                , id: 'shardInfo'
                , content: _autoCreateFormHtml
                , btnAlign: 'r'
                , shade: 0.6
                , success: function () {
                    $("input[name='id']").attr({ "disabled": "disabled" }).parent().parent().show();
                    form.render();
                    form.val("autoCreateForm", {
                        id: data.id
                        , name: data.name
                        , nameFormat: data.nameFormat
                        , format: data.format
                        , aliasFormat: data.aliasFormat
                        , timeField: data.timeField
                    })
                    form.on('submit(autoCreateFormSubmit)', function (data) {
                        vhttp.ajax("/.index-tools-auto-create/doc/" + data.field["id"], "PUT", JSON.stringify({
                            "name": data.field["name"]
                            , "nameFormat": data.field["nameFormat"]
                            , "format": data.field["format"]
                            , "aliasFormat": data.field["aliasFormat"]
                            , "timeField": data.field["timeField"]
                        }), function (data) {
                            vhttp.ajax("/.index-tools-auto-create/_refresh", "GET", function () {
                                autoCreateTable.reload();
                                layer.closeAll();
                            })
                        })
                        return false;
                    });
                    $("#autoCreateFormClose").click(function () {
                        layer.closeAll();
                    });
                }
            })
        }
    });

    table.on('toolbar(autoCreate)', function (obj) {
        if (obj.event === 'add') {
            layer.open({
                type: 1
                , area: '800px'
                , offset: 't'
                , id: 'shardInfo'
                , content: _autoCreateFormHtml
                , btnAlign: 'r'
                , shade: 0.6
                , success: function () {
                    $("input[name='id']").parent().parent().hide();
                    form.render()
                    form.on('submit(autoCreateFormSubmit)', function (data) {
                        vhttp.ajax("/.index-tools-auto-create/doc", "POST", JSON.stringify({
                            "name": data.field["name"]
                            , "nameFormat": data.field["nameFormat"]
                            , "format": data.field["format"]
                            , "aliasFormat": data.field["aliasFormat"]
                            , "timeField": data.field["timeField"]
                        }), function (data) {
                            vhttp.ajax("/.index-tools-auto-create/_refresh", "GET", function () {
                                autoCreateTable.reload();
                                layer.closeAll();
                            })
                        })
                        return false;
                    });
                    $("#autoCreateFormClose").click(function () {
                        layer.closeAll();
                    });
                }
            })
        }
    })
});