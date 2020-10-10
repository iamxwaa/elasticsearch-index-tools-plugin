layui.use(['layer', 'form', 'table', 'vhttp', 'vmodule'], function () {
    var form = layui.form;
    var table = layui.table;
    var vhttp = layui.vhttp;
    var vmodule = layui.vmodule;

    var _userFormHtml = `
<form class="layui-form layui-form-pane" action="" lay-filter="userForm" style="padding: 10px;">
    <div class="layui-form-item">
        <label class="layui-form-label">ID</label>
            <div class="layui-input-block">
            <input type="text" name="userid" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">用户名</label>
            <div class="layui-input-block">
            <input type="text" name="username" lay-verify="required" placeholder="请输入用户名" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">密码</label>
            <div class="layui-input-block">
            <input type="password" name="password" lay-verify="required" placeholder="请输入密码" autocomplete="off" class="layui-input">
            <input type="password" name="oldP" style="display: none;">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">确认密码</label>
            <div class="layui-input-block">
            <input type="password" name="password2" lay-verify="required|confirmpwd" placeholder="请输入密码" autocomplete="off" class="layui-input">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">索引权限</label>
        <div class="layui-input-block">
            <input type="checkbox" name="adminright[create]" title="创建" checked="">
            <input type="checkbox" name="adminright[delete]" title="删除" checked="">
        </div>
    </div>
    <div class="layui-form-item">
        <label class="layui-form-label">数据权限</label>
        <div class="layui-input-block">
            <input type="checkbox" name="dataright[read]" title="读取" checked="">
            <input type="checkbox" name="dataright[write]" title="写入" checked="">
        </div>
    </div>
    <div class="layui-form-item layui-form-text">
        <label class="layui-form-label">查询权限</label>
        <div class="layui-input-block">
            <textarea name="settings" placeholder="请输入内容" class="layui-textarea" rows="8" style="resize: none;"></textarea>
        </div>
        <div class="layui-form-mid layui-word-aux">
            1. '*' 表示可访问全部索引,为空表示无索引访问权限<br>
            2. 'test*' 表示可访问test开头的索引(仅支持前缀匹配)<br>
            3. 'test:field1,field2,field3' 表示可访问test索引的field1,field2,field3字段(预留,字段控制未实现)<br>
            4. 多个索引以换行符(回车)分割
        </div>
    </div>
    <div class="layui-form-item">
        <div class="layui-input-block" style="margin-left: 510px;">
            <button type="submit" class="layui-btn" lay-submit="" lay-filter="userFormSubmit">提交</button>
            <button type="button" class="layui-btn layui-btn-primary" id="userFormClose">关闭</button>
        </div>
    </div>
</form>`;



    var userTable = table.render({
        elem: '#userTable'
        , toolbar: '#userToolBar'
        , defaultToolbar: []
        , height: window.screen.availHeight - 300
        , page: true
        , url: "/_itools/user"
        , cols: [[
            { field: 'id', title: 'ID', width: 300 }
            , { field: 'username', title: '用户名', width: 220, sort: true }
            , { field: 'password', title: '密码', width: 200, hide: true }
            , { field: 'adminright', title: '索引权限', width: 150 }
            , { field: 'dataright', title: '数据权限', width: 150 }
            , { field: 'settings', title: '权限' }
            , { width: 178, align: 'center', toolbar: '#userTableBar' }
        ]]
        , parseData: function (res) {
            for (var i in res.data) {
                res.data[i].oldP = res.data[i].password;
            }
        }
    });

    var setVerifyForm = function (form) {
        form.verify({
            confirmpwd: function (value, item) {
                if (value != $("input[name='password']").val()) {
                    return "两次密码不一致";
                }
            }
        })
    }

    table.on('tool(user)', function (obj) {
        var data = obj.data;
        if (obj.event === 'detail') {
            layer.msg(data.settings.replace(/\n/g, "<br>"));
        } else if (obj.event === 'del') {
            vmodule.rConfirm({
                msg: "确定删除: " + data.username + " ?"
                , url: "/_itools/user/" + data.id
                , method: "DELETE"
                , callback: function () {
                    userTable.reload();
                    layer.closeAll();
                }
            })
        } else if (obj.event === 'edit') {
            layer.open({
                type: 1
                , area: '800px'
                , offset: 't'
                , id: 'shardInfo'
                , content: _userFormHtml
                , btnAlign: 'r'
                , shade: 0.6
                , success: function () {
                    $("input[name='userid']").attr({ "disabled": "disabled" }).parent().parent().show();
                    $("input[name='username']").attr({ "disabled": "disabled" });
                    form.render();
                    setVerifyForm(form);
                    var adminright1 = undefined == data.adminright ? false : (data.adminright.indexOf("create") != -1);
                    var adminright2 = undefined == data.adminright ? false : (data.adminright.indexOf("delete") != -1);
                    var dataright1 = undefined == data.dataright ? false : (data.dataright.indexOf("write") != -1);
                    var dataright2 = undefined == data.dataright ? false : (data.dataright.indexOf("read") != -1);
                    form.val("userForm", {
                        userid: data.id
                        , username: data.username
                        , password: data.password
                        , password2: data.password
                        , settings: data.settings
                        , oldP: data.oldP
                        , "adminright[create]": adminright1
                        , "adminright[delete]": adminright2
                        , "dataright[write]": dataright1
                        , "dataright[read]": dataright2
                    })
                    form.on('submit(userFormSubmit)', function (data) {
                        var adminright = []
                        if (data.field["adminright[create]"] == "on") {
                            adminright.push("create")
                        }
                        if (data.field["adminright[delete]"] == "on") {
                            adminright.push("delete")
                        }
                        var dataright = []
                        if (data.field["dataright[write]"] == "on") {
                            dataright.push("write")
                        }
                        if (data.field["dataright[read]"] == "on") {
                            dataright.push("read")
                        }
                        var pass = data.field.password == data.field.oldP ? data.field.password : $.md5(data.field.password);
                        vhttp.ajax("/_itools/user/" + data.field.userid + "/" + data.field.username, "PUT", JSON.stringify({
                            "username": data.field.username
                            , "password": pass
                            , "adminright": adminright.join(",")
                            , "dataright": dataright.join(",")
                            , "settings": data.field.settings
                        }), function (data) {
                            userTable.reload();
                            layer.closeAll();
                        })
                        return false;
                    });
                    $("#userFormClose").click(function () {
                        layer.closeAll();
                    });
                }
            })
        }
    });

    table.on('toolbar(user)', function (obj) {
        if (obj.event === 'add') {
            layer.open({
                type: 1
                , area: '800px'
                , offset: 't'
                , id: 'shardInfo'
                , content: _userFormHtml
                , btnAlign: 'r'
                , shade: 0.6
                , success: function () {
                    $("input[name='userid']").parent().parent().hide();
                    form.render();
                    setVerifyForm(form);
                    form.on('submit(userFormSubmit)', function (data) {
                        var adminright = []
                        if (data.field["adminright[create]"] == "on") {
                            adminright.push("create");
                        }
                        if (data.field["adminright[delete]"] == "on") {
                            adminright.push("delete");
                        }
                        var dataright = []
                        if (data.field["dataright[write]"] == "on") {
                            dataright.push("write");
                        }
                        if (data.field["dataright[read]"] == "on") {
                            dataright.push("read");
                        }
                        vhttp.ajax("/_itools/user/0/" + data.field.username, "PUT", JSON.stringify({
                            "username": data.field.username
                            , "password": $.md5(data.field.password)
                            , "adminright": adminright.join(",")
                            , "dataright": dataright.join(",")
                            , "settings": data.field.settings
                        }), function (data) {
                            userTable.reload();
                            layer.closeAll();
                        })
                        return false;
                    });
                    $("#userFormClose").click(function () {
                        layer.closeAll();
                    });
                }
            })
        }
    })
});