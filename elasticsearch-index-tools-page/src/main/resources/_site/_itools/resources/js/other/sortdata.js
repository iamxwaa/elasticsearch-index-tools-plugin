layui.use(['tree', 'form', 'vhttp', 'vtools', 'vtpl', 'formSelects'], function () {
    var tree = layui.tree;
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vtpl = layui.vtpl;
    var formSelects = layui.formSelects;

    var indices;
    vhttp.ajax("/_cluster/state", "GET", function (d) {
        var indexField = {};
        var keysData = [];
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

    // table.render({
    //     elem: '#indexTable'
    //     , data: []
    //     , page: false
    //     , limit: 5000
    //     , cols: [[
    //         { field: 'indexName', title: '索引名称', width: '50%', style: 'node-table-info4', fixed: 'left',align: 'center' }
    //         , { field: 'process', title: '进度', width: '50%' ,align: 'center'}
    //     ]]
    // });

    form.on('submit(commitTemplate)', function (data) {
        var indexNames = data.field.indexData.split(",");
        var isDelete = data.field.method;
        vtpl.render(dataTable, indexNames, "indexTable");
        form.render();
        for (var i = 0;i < indexNames.length;i++) {
            (function(arg) {
                var tmpData = {};
                var oldName = indexNames[i];
                console.log(oldName)
                var sourceData = {"index":oldName};
                var destData = {"index": oldName+"-bakindex"};
                tmpData.source = sourceData;
                tmpData.dest = destData;
                //reindex 复制数据
                var spanId = oldName.replace(/./g,'_')+"pro";
                $("#"+spanId).html("进行中");
                vhttp.ajax("/_reindex", "POST", JSON.stringify(tmpData), function (rData) {
                    console.log(rData);
                    $("#"+spanId).html("新索引复制成功");
                    sleep(2000);
                    vhttp.ajax("/"+oldName, "DELETE", function (dData) {
                        console.log(dData)
                        if (dData["acknowledged"] == true){
                            $("#"+spanId).html("旧索引删除成功");
                            var tmpData1 = {};
                            var sourceData1 = {"index":oldName+"-bakindex"};
                            var destData1 = {"index": oldName};
                            tmpData1.source = sourceData1;
                            tmpData1.dest = destData1;
                            sleep(2000);
                            vhttp.ajax("/_reindex", "POST", JSON.stringify(tmpData1), function (rData2) {
                                console.log(rData2);
                                $("#"+spanId).html("旧索引还原成功");
                                if (isDelete == "yes") {
                                    sleep(2000);
                                    vhttp.ajax("/"+oldName+"-bakindex", "DELETE", function (dData2) {
                                        console.log(dData2);
                                        if (dData2["acknowledged"] == true){
                                            $("#"+spanId).html("新索引删除成功");
                                        }
                                    });
                                }
                            });
                        }
                    });
                });
            }(i));
        }
        return false;
    });

    function sleep(d){
        for(var t = Date.now();Date.now() - t <= d;);
    }
});