layui.use(['tree', 'form', 'vhttp', 'vmenu', 'vmodule'], function () {
    var tree = layui.tree;
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vmenu = layui.vmenu;
    var vmodule = layui.vmodule;

    var _template = {};
    var _current_title = localStorage.getItem("_current_title");
    var _fileRowNumber = 0;
    function getFieldRow(number) {
        return `
<div class="layui-form-item">
    <div class="layui-inline">
        <label class="layui-form-label">字段名称</label>
        <div class="layui-input-inline">
            <input type="text" name="fieldName`+ number + `" lay-verify="required" autocomplete="off"
                class="layui-input">
        </div>
    </div>
    <div class="layui-inline">
        <label class="layui-form-label">字段类型</label>
        <div class="layui-input-block">
            <select name="fieldType`+ number + `" lay-verify="required">
                <option value="text">text</option>
                <option value="keyword">keyword</option>
                <option value="long">long</option>
                <option value="integer">integer</option>
                <option value="short">short</option>
                <option value="byte">byte</option>
                <option value="double">double</option>
                <option value="float">float</option>
                <option value="half_float">half_float</option>
                <option value="scaled_float">scaled_float</option>
                <option value="date">date</option>
                <option value="boolean">boolean</option>
                <option value="binary">binary</option>
                <option value="integer_range">integer_range</option>
                <option value="float_range">float_range</option>
                <option value="long_range">long_range</option>
                <option value="double_range">double_range</option>
                <option value="date_range">date_range</option>
                <option value="object">object</option>
                <option value="nested">nested</option>
                <option value="geo_point">geo_point</option>
                <option value="geo_shape">geo_shape</option>
                <option value="ip">ip</option>
                <option value="completion">completion</option>
                <option value="token_count">token_count</option>
                <option value="murmur3">murmur3</option>
            </select>
        </div>
    </div>
    <div class="layui-inline">
        <label class="layui-form-label">其他配置</label>
        <div class="layui-input-inline">
            <input type="text" name="fieldOther`+ number + `" placeholder="{'format': 'epoch_millis'}"
                autocomplete="off" class="layui-input">
        </div>
    </div>
</div>
`
    }

    vhttp.ajax("/_template", "GET", function (d) {
        _template = d;
        var treelist = [];
        for (var i in d) {
            treelist.push({ "title": i });
        }
        tree.render({
            elem: '#template-tree',
            data: treelist,
            click: function (obj) {
                _current_title = obj.data.title;
                $("#currentTemplate").html(_current_title);
                $("#rspbodyEdit").hide();
                $("#rspbody").show().html("...").jsonViewer(_template[_current_title]);
            }
        });
        if ("" != _current_title && undefined != _current_title) {
            $("#currentTemplate").html(_current_title);
            $("#rspbody").show().html("...").jsonViewer(_template[_current_title]);
            $.removeCookie("_current_title");
        }
    })
    $("#editTemplate").click(function () {
        if (undefined == _current_title) {
            return;
        }
        $("#rspbody").hide();
        $("#rspbodyEdit").show().children("textarea").val(JSON.stringify(_template[_current_title], null, "\t"));
    })

    $("#saveTemplate").click(function () {
        if (undefined == _current_title) {
            return;
        }
        var json = $("#rspbodyEdit").children("textarea").val();
        if ("" == json) {
            return;
        }
        vhttp.ajax("/_template/" + _current_title, "PUT", json, function (data) {
            localStorage.setItem("_current_title", _current_title);
            vmenu.reloadCurrent();
        })
    })

    $("#deleteTemplate").click(function () {
        if (undefined != _current_title) {
            vmodule.rConfirm2({
                msg: "确定删除 " + _current_title + " ?"
                , url: "/_template/" + _current_title
                , method: "DELETE"
            })
        }
    })

    form.on('submit(commitTemplate)', function (data) {
        var t = {};
        t.index_patterns = data.field.template;
        t.settings = { number_of_shards: data.field.number_of_shards, number_of_replicas: data.field.number_of_replicas };
        var properties = {};
        for (var i in data.field) {
            if (i.startsWith("fieldName")) {
                var index = i.substring(9);
                var fieldName = data.field["fieldName" + index];
                var fieldType = data.field["fieldType" + index];
                var fieldOther = data.field["fieldOther" + index];
                if (fieldOther.startsWith("{")) {
                    properties[fieldName] = JSON.parse(fieldOther);
                    properties[fieldName]["type"] = fieldType;
                } else {
                    properties[fieldName] = { type: fieldType };
                }
            }
        }
        t.mappings = {};
        t.mappings[data.field.type] = {
            _source: {
                enabled: data.field.enableSource == "1"
            },
            "properties": properties
        }
        var templateName = data.field.templateName;
        vhttp.ajax("/_template/" + templateName, "PUT", JSON.stringify(t), function (data) {
            localStorage.setItem("_current_title", templateName);
            vmenu.reloadCurrent();
        })
        return false;
    });

    $("input[value='+']").click(function () {
        var box = $(this).parent().parent().parent();
        box.append(getFieldRow(_fileRowNumber++));
        form.render();
    })
    $("input[value='-']").click(function () {
        if (_fileRowNumber == 0) {
            return;
        }
        var box = $(this).parent().parent();
        box.siblings().last().remove();
        _fileRowNumber--;
        form.render();
    })

    form.render();
});