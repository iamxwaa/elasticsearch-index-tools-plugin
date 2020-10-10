layui.use(['form', 'vhttp', 'vmenu', 'vmodule', 'formSelects', 'vtools'], function () {
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vtools = layui.vtools
    var vmenu = layui.vmenu;
    var vmodule = layui.vmodule;
    var formSelects = layui.formSelects;

    var _fileRowNumber = 0;
    function getFieldRow(number) {
        return `
                <div class="layui-form-item">
                    <label class="layui-form-label">子统计`+ number + `</label>
                    <div class="layui-inline" style="width:120px;">
                        <select name="subStatisticType`+ number + `">
                            <option value="">请选择</option>
                            <option value="terms">分组统计</option>
                            <option value="cardinality">去重</option>
                            <option value="sum">加和</option>
                            <option value="count">统计</option>
                        </select>
                    </div>
                    <div class="layui-inline">
                        <select name="subFieldData`+ number + `" id="subFieldData`+ number + `">
                            <option value=""></option>
                        </select>
                    </div>
                </div>
`
    }

    // $("input[value='+']").click(function () {
    $("#sumBtn3").click(function () {
        var box = $(this).parent().parent().parent();
        box.append(getFieldRow(_fileRowNumber++));
        form.render();
        getSelectFieldHtml("subFieldData"+(_fileRowNumber-1))
    })
    $("#sumBtn4").click(function () {
        if (_fileRowNumber == 0) {
            return;
        }
        var box = $(this).parent().parent();
        box.siblings().last().remove();
        _fileRowNumber--;
        form.render();
    })

    form.on('submit(commitTemplate)', function (data) {
        if (data.field.indexData == ""){
            layer.msg("请选择索引！");
            return false;
        }
        var t = {};
        console.log(data);
        t.indexNames = data.field.indexData;
        t.start_time = data.field.start_time;
        t.end_time = data.field.end_time;
        t.statisticType = data.field.statisticType;
        t.fieldData = data.field.fieldData;
        var mustData = [];
        var subStatisticData = [];
        for (var i in data.field){
            if (i.startsWith("mustType")) {
                if (i == "mustType"){
                    mustData.push({"mustType": data.field.mustType, "mustField": data.field.mustField, "termType": data.field.termType,
                        "mustFieldData": data.field.mustFieldData,"gtType":data.field.gtType, "gtData":data.field.gtData,
                        "ltType": data.field.ltType, "ltData":data.field.ltData});
                }else{
                    var index = i.replace("mustType","");
                    mustData.push({"mustType": data.field["mustType"+index], "mustField": data.field["mustField"+index], "termType": data.field["termType"+index],
                        "mustFieldData": data.field["mustFieldData"+index],"gtType":data.field["gtType"+index], "gtData":data.field["gtData"+index],
                        "ltType": data.field["ltType"+index], "ltData":data.field["ltData"+index]});
                }
            }
            if (i.startsWith("subStatisticType")){
                    var index = i.replace("subStatisticType","");
                subStatisticData.push({"subStatisticType":data.field["subStatisticType"+index], "subFieldData":data.field["subFieldData"+index]});
            }
        }
        t.mustData = mustData;
        t.subStatisticData = subStatisticData;
        console.log(t);
        vhttp.ajax("/_itools/statistic", "POST", JSON.stringify(t), function (data) {
            console.log(data);
            $("#queryData").jsonViewer(JSON.parse(data.requestSource));
            $("#queryResult").jsonViewer(data.aggData);

        });
        return false;
    });

    var _fileRowNumber1 = 0;
    function getFieldRow1(number) {
        return `
<div class="layui-form-item">
    <div class="layui-inline" style="width:110px;">
        <select name="mustType`+ number + `" lay-verify="required">
         <option value="must">must</option>
         <option value="must_not">must_not</option>
         <option value="should">should</option>
        </select>
       </div>
       <div class="layui-inline" style="width:120px;">
        <select name="mustField`+ number + `"  id="mustField`+number+`">
        </select>
       </div>
       <div class="layui-inline"  style="width:80px;">
        <select name="termType`+ number + `" lay-filter="termTypeFilter">
         <option value="term">term</option>
         <option value="range">range</option>
        </select>
       </div>
       <div class="layui-inline" style="width:120px;" id="fieldDataDIv`+ number + `">
        <input type="text" name="mustFieldData`+ number + `" id="mustFieldData`+ number + `" autocomplete="off" class="layui-input">
       </div>
       <div class="layui-inline"  style="width:80px;display:none" id="gtDiv`+ number + `">
        <select name="gtType`+ number + `" >
         <option value="gt">gt</option>
         <option value="gte">gte</option>
        </select>
       </div>
       <div class="layui-inline" style="width:120px;display:none" id="gtDataDiv`+ number + `">
        <input type="text" name="gtData`+ number + `" autocomplete="off" class="layui-input">
       </div>
       <div class="layui-inline"  style="width:80px;display:none" id="ltDiv`+ number + `">
        <select name="ltType`+ number + `" >
         <option value="lt">lt</option>
         <option value="lte">lte</option>
        </select>
       </div>
       <div class="layui-inline" style="width:120px;display:none" id="ltDataDiv`+ number + `">
        <input type="text" name="ltData`+ number + `" autocomplete="off" class="layui-input">
       </div>
</div>
`
    }

    $("#sumBtn1").click(function () {
        var box = $(this).parent().parent().parent();
        box.append(getFieldRow1(_fileRowNumber1++));
        form.render();
        getSelectFieldHtml("mustField"+(_fileRowNumber1-1));
    })
    $("#sumBtn2").click(function () {
        if (_fileRowNumber1 == 0) {
            return;
        }
        var box = $(this).parent().parent();
        box.siblings().last().remove();
        _fileRowNumber1--;
        form.render();
    })

    form.on('select(termTypeFilter)', function(data){
        var name = data.elem.name;
        var dataValue = data.value;
        if (name == "termType"){
            if(dataValue == 'range'){
                $("#fieldDataDIv").hide();
                $("#mustFieldData").val("");
                $("#gtDiv").show();
                $("#gtDataDiv").show();
                $("#ltDiv").show();
                $("#ltDataDiv").show();
            }else{
                $("#fieldDataDIv").show();
                $("#gtDiv").hide();
                $("#gtDataDiv").hide();
                $("#gtData").val("");
                $("#ltDiv").hide();
                $("#ltDataDiv").hide();
                $("#ltData").val("");
            }
        }else{
            var indx = name.replace("termType","");
            if(dataValue == 'range'){
                $("#fieldDataDIv"+indx).hide();
                $("#mustFieldData"+indx).val("");
                $("#gtDiv"+indx).show();
                $("#gtDataDiv"+indx).show();
                $("#ltDiv"+indx).show();
                $("#ltDataDiv"+indx).show();
            }else{
                $("#fieldDataDIv"+indx).show();
                $("#gtDiv"+indx).hide();
                $("#gtDataDiv"+indx).hide();
                $("#gtData"+indx).val("");
                $("#ltDiv"+indx).hide();
                $("#ltDataDiv"+indx).hide();
                $("#ltData"+indx).val("");
            }
        }
    });

    layui.use('laydate', function(){
        var laydate = layui.laydate;
        //时间选择器
        laydate.render({
            elem: '#start_time'
            ,type: 'datetime'
        });
        //日期时间选择器
        laydate.render({
            elem: '#end_time'
            ,type: 'datetime'
        });
    });
    var keysData = [];
    var indexField = {};
    vhttp.ajax("/_cluster/state", "GET", function (d) {

        var indices = d.metadata.indices;
        for (var i in indices) {
            if(indices[i]["mappings"]["logs"] != undefined) {
                keysData.push({"name": i, "value": i});

                var fields = indices[i]["mappings"]["logs"]["properties"];
                var fieldsList = [];
                for (var j in fields) {
                    fieldsList.push(j);
                }
                indexField[i] = fieldsList;
            }
        }
        // localStorage.setItem("indexField", JSON.stringify(indexField));
        form.render();
        formSelects.data('indexData', 'local', {
            arr: keysData
        });
    });
    formSelects.render('indexData',{
        skin: "normal",
        autoRow: true,
        radio: false,
        direction: "auto"
    });
    var selectedField = [];
    formSelects.on('indexData', function(id, vals, val, isAdd, isDisabled){
        var innerSelect = [];
        var selected = vals;
        if (isAdd){
            selected.push(val);
        }else{
            selected.splice($.inArray(val,selected),1);
        }

        for (var i=0;i<selected.length;i++){
            var selectedValue = indexField[selected[i].value];
            if (innerSelect.length == 0){
                innerSelect = selectedValue;
            }else{
                innerSelect = innerSelect.filter(function(v){
                    return selectedValue.indexOf(v) !== -1;
                });
            }
        }
        selectedField = innerSelect;
        // localStorage.setItem("selectedField", JSON.stringify(selectedField));
        getSelectFieldHtml("fieldData");
        getSelectFieldHtml("mustField");
        // getSelectFieldHtml("subFieldData")
        // form.render("select");
        return true;
    });

    function getSelectFieldHtml(id){
        var selectedHtmlString = [];
        selectedHtmlString.push('<option value=""></option>');
        for (var i in selectedField) {
            selectedHtmlString.push('<option value="' + selectedField[i] + '">' + selectedField[i] + '</option>');
        }
        $("#"+id).html(selectedHtmlString.join(""));
        form.render("select");
    }

});