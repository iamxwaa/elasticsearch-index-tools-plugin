layui.use(['tree', 'vhttp'], function () {
    var tree = layui.tree;
    var vhttp = layui.vhttp;

    var _alias = {};

    var refresh = function () {
        $("#index-tree").html("...");
        vhttp.ajax("/_alias", "GET", function (d) {
            _alias = d;
            var treelist = [];
            for (var i in d) {
                var children = [];
                for (var j in d[i].aliases) {
                    children.push({ "title": j, "parent": i });
                }
                if (children.length != 0) {
                    treelist.push({ "title": i, "children": children });
                }
            }
            tree.render({
                elem: '#index-tree'
                , data: treelist
                , click: function (obj) {
                    if (obj.data.parent != undefined) {
                        $("#rspbody1").html("...").jsonViewer(_alias[obj.data.parent].aliases[obj.data.title]);
                    }
                }
            });
        })
    };
    refresh()
    $("#index-tree-refresh").click(refresh)
});