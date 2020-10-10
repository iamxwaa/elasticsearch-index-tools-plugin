var _template = {};
var current_title_key = "_current_template_title";
var _current_title = localStorage.getItem(current_title_key);
var _current_tab_key = "_current_generate_tab";
var _current_tab = localStorage.getItem(_current_tab_key);
function loadPage(vmenu,millseconds) {
    //延迟加载后获取更新后的索引数据
    window.setTimeout(function(){ vmenu.reloadCurrent();},millseconds)
}
function showTips(title, message) {
    layui.use('layer', function () {
        var layer = layui.layer
        layer.open({
            title: title
            ,content: message
        });
    })
}

layui.use(['tree', 'form','element', 'vhttp', 'vmenu', 'vmodule'], function () {
    var tree = layui.tree;
    var form = layui.form;
    var element = layui.element;
    var vhttp = layui.vhttp;
    var vmenu = layui.vmenu;
    var vmodule = layui.vmodule;
    element.tabChange('tab_filter', _current_tab);
    //监听Tab切换
    element.on('tab(tab_filter)', function(){
        localStorage.setItem(_current_tab_key, this.getAttribute('lay-id'));
    });
    var _current_template = "";
    var templateUrl = "/_itools/generatetest/template"
    var templatePoolUrl = "/_itools/generatetest/pool"
    var generateUrl = "/_itools/generatetest/generate"
    var currentPage = "/page/other/generatedata";
    var _indices = {}
    vhttp.ajax(templateUrl, "POST", function (d) {
        _template = d.data;
        var treelist = [];
        for (var i in _template) {
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
                localStorage.setItem(current_title_key, _current_title);
            }
        });

        if ("" != _current_title && undefined != _current_title) {
            $("#currentTemplate").html(_current_title);
            $("#rspbody").show().html("...").jsonViewer(_template[_current_title]);
            $.removeCookie(current_title_key);
        }

        var allTemplate = ['<option value="">选择模板</option>']
        for (var i in _template) {
            allTemplate.push('<option value="' + i + '">' + i + '</option>')
        }
        $("#template").html(allTemplate.join(""))
        form.render();

        form.on('select(template)', function (data) {
            _current_template = data.value
            if(_current_template in _indices) {
                var allIndex = []
                for (var index in _indices[_current_template]) {
                    allIndex.push('<option value="' + index + '">' + index + '</option>')
                }
                allIndex.sort(function (a, b) { return b>=a?1:-1 });
                allIndex.unshift('<option value="">' + '选择索引' + '</option>')
                $("#index").html(allIndex.join(""));
                form.render();
            } else {
                vhttp.ajax(data.value+"-*", "GET", function (d) {
                    _indices[_current_template] = d;
                    var allIndex = []
                    for (var index in d) {
                        allIndex.push('<option value="' + index + '">' + index + '</option>')
                    }
                    allIndex.sort(function (a, b) { return b>=a?1:-1 });
                    allIndex.unshift('<option value="">' + '选择索引' + '</option>');
                    $("#index").html(allIndex.join(""));
                    form.render();
                });
            }

        });
    })

    function extractedFields(poolFields) {
        tree.render({
            elem: '#template-pool-tree',
            onlyIconControl:true,
            data: [{title: "generatetest-pool",spread:true, children: poolFields}],
            click: function (obj) {
                var title = obj.data.title;
                var currentField = {}
                title!="generatetest-pool"?(currentField[title] = pool[title]):(currentField=pool);
                $("#rsPoolBodyArea").val(JSON.stringify(currentField, null, "\t"));
                $("#rsPoolbodyEdit").hide();
                $("#rsPoolpbody").show().html("...").jsonViewer(currentField,{ collapsed: false, rootCollapsable: false, withLinks: false});
            }
        });
    }

    var poolFields=[];
    var pool ={}
    vhttp.ajax(".index-tools-generatetest-pool/test/_search", "POST", function (d) {
        var res = d.hits.hits;
        for (var i = 0; i < res.length; i++) {
            pool[res[i]._id]=res[i]._source.values;
            poolFields.push({"title":res[i]._id});
        }
        extractedFields(poolFields);

        $("#rsPoolpbody").show().html("...").jsonViewer(pool,{ collapsed: false, rootCollapsable: false, withLinks: false});
        $("#rsPoolbodyEdit").children("textarea").val(JSON.stringify(pool, null, "\t"));
    })
    extractedFields(poolFields);

    $("#editTemplatePool").click(function () {
        $("#rsPoolpbody").hide();
        $("#rsPoolbodyEdit").show();
    })

    $("#saveTemplatePool").click(function () {
        var json = $("#rsPoolbodyEdit").children("textarea").val();
        if ("" == json) {
            return;
        }
        vhttp.ajax(templatePoolUrl, "PUT", json, function (data) {
            if(data || data=="true") {
                showTips("修改内置数据","操作成功");
            }
            localStorage.setItem(current_title_key, _current_title);
            loadPage(vmenu,1000);
        })
    })

    form.on('select(index)', function (data) {
        var index = data.value;
        if(index==''){return;}
        var monthOrDay = index.substr(_current_template.length+1,index.length);
        var dates = [];
        var date = new Date(monthOrDay.substr(0,7).replace(/\./g,"/"));
        var fullYear = date.getFullYear();
        var month = date.getMonth()+1;
        if(monthOrDay.length==7){
            dates.push(fullYear+"-"+month);
            var days = new Date(fullYear,month,0).getDate();
            for (var i = 1; i < days+1; i++) {
                dates.push(fullYear+"-"+month+"-"+(i<10?"0"+i:i));
            }
        } else {
            dates.push(fullYear+"-"+month+"-"+date.getDay());
        }
        var allDate = [];
        for (var i=0;i<dates.length;i++) {
            allDate.push('<option value="' + dates[i] + '">' + dates[i] + '</option>')
        }
        $("#date").html(allDate.join(""));
        form.render();


    });

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
        vhttp.ajax(templateUrl +"/"+ _current_title, "PUT", json, function (data) {
            if(data || data=="true") {
                showTips("修改数据模板","操作成功");
            }
            localStorage.setItem(current_title_key, _current_title);
            loadPage(vmenu,800);
        })
    })

    $("#deleteTemplate").click(function () {
        if (undefined != _current_title) {

            vmodule.rConfirm2({
                msg: "确定删除 " + _current_title + " ?"
                , url: templateUrl +"/"+ _current_title
                , method: "DELETE"
            })

        }
    })

    form.on('submit(commitGenerate)', function (data) {
        var t = {};
        var templateName = data.field.template;
        t.template = _template[templateName].data_json;
        t.settings = [{ index: data.field.index, rows: data.field.rows, date: data.field.date,template:_template[templateName]}];
        var total = data.field.rows;
        vhttp.ajax(generateUrl, "POST", JSON.stringify(t), function (data) {
            if(total<=20000 && (data || data=="true")) {
                showTips("测试数据","操作成功");
            }
            localStorage.setItem(current_title_key, templateName);
            // loadPage(vmenu);
        })
        if(total>20000) {
            showTips("测试数据","生成中, 请耐心等待...");
        }
        return false;
    });

    form.on('submit(commitTemplate)', function (data) {
        var templateId = data.field.templateId.trim();
        var cron = data.field.cron;
        var templateJson = JSON.parse(data.field.templateBody);
        if("data_json" in templateJson) {
            templateJson = templateJson.data_json;
        }
        var param = {data_json:templateJson,index_group:templateId,cron:cron}
        // templateJson.index_group = templateId;
        vhttp.ajax(templateUrl +"/"+ templateId, "PUT", JSON.stringify(param), function (data) {
            if(data || data=="true") {
                showTips("新建数据模板","添加成功");
            }
            _current_title = templateId;
            localStorage.setItem(current_title_key, _current_title);
            loadPage(vmenu,800);
        })
        return false;
    });

    form.render();
});