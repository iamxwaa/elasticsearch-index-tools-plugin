layui.use(['tree', 'form', 'vhttp', 'vtools', 'vtpl'], function () {
    var tree = layui.tree;
    var form = layui.form;
    var vhttp = layui.vhttp;
    var vtools = layui.vtools;
    var vtpl = layui.vtpl;

    var searchHits = []

    var renderTable = function (d) {
        var hits = d.hits.hits;
        searchHits = hits;

        vtpl.render2({
            tpl: "查询 {{ d._shards.total }} 个分片中用的 {{ d._shards.successful }} 个, {{ d.hits.total }} 命中. 耗时 {{ d.took }} ms"
            , data: d
            , vid: "rspheader"
        });

        if (hits.length == 0) {
            return;
        }

        vtpl.render(dataTable, hits, "rspbody");
        $("td[name='spread']").click(function () {
            var idIndex = $(this).attr("data");
            var tr = $(this).parent().next();
            var jsonView = $("#jsonView");
            var state = jsonView.attr("data");
            if (undefined == state) {
                tr.html("<td colspan='2'><div id='jsonView'></div></td>");
                jsonView = $("#jsonView");
                jsonView.jsonViewer(searchHits[idIndex]);
                jsonView.attr({ "data": idIndex });
            } else if (idIndex == state) {
                jsonView.parent().remove();
            } else {
                jsonView.parent().remove();
                tr.html("<td colspan='2'><div id='jsonView'></div></td>");
                jsonView = $("#jsonView");
                jsonView.jsonViewer(searchHits[idIndex]);
                jsonView.attr({ "data": idIndex });
            }
        })
    }

    var badAction = function (d) {
        if (typeof d == "object") {
            if (undefined == d.responseJSON) {
                vtools.renderHtml("rspbody", d.responseText, vtools.formatText);
            } else {
                vtools.renderJson("rspbody", d.responseJSON);
            }
        } else {
            $("#rspbody").html(vtools.formatText(d));
        }
    }

    vhttp.ajax("/_cluster/state", "GET", function (d) {
        var indices = d.routing_table.indices;
        var treelist = [];
        for (var i in indices) {
            treelist.push({ "title": i });
        }
        tree.render({
            elem: '#index-tree',
            data: treelist,
            click: function (obj) {
                $("#rspheader").html("...");
                $("#rspbody").html("...");
                vhttp.ajax("/" + obj.data.title + "/_search?size=50", "GET", "", renderTable, badAction);
            }
        });

        vtpl.render(optionList, d.metadata.indices, "allIndices");
        form.on('select(allIndices)', function (data) {
            $("#rspheader").html("...");
            $("#rspbody").html("...");
            vhttp.ajax("/" + data.value + "/_search?size=50", "GET", "", renderTable, badAction);
        });
        form.render();
    })
});