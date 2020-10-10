layui.use(['laypage', 'element', 'vmenu', 'vtools', 'vhttp', 'vmodule'], function () {
    var laypage = layui.laypage;
    var element = layui.element;
    var vmenu = layui.vmenu;
    var vtools = layui.vtools;
    var vhttp = layui.vhttp;
    var vmodule = layui.vmodule;


    var PAGE_LIMIT = 4;

    var _routing_nodes = {};
    var _routing_nodes_ids = [];
    var _routing_table_indices = [];
    var _routing_table_indices_filtered = [];
    var _indices_metadata = [];
    var _indices_stats = [];
    var _loadPage_enable = false;

    var updateStatus = function () {
        vhttp.ajax("/_cluster/health", "GET", function (d) {
            $("#nodeCount").text(d.number_of_nodes);
            if (d.unassigned_shards == 0) {
                $("#shardsInfo").parent().hide();
            }
            var loading = '<i class="layui-icon layui-icon-loading layui-icon layui-anim layui-anim-rotate layui-anim-loop"></i>';
            var infolist = [];
            infolist.push('初始化分片数: ' + d.initializing_shards);
            infolist.push('无效分片数: ' + d.unassigned_shards);
            if (0 != d.initializing_shards) {
                infolist[0] = infolist[0] + "&nbsp;&nbsp;" + loading;
                infolist[1] = infolist[1] + "&nbsp;&nbsp;" + loading;
                _loadPage_enable = true;
            } else {
                vtools.removeInterval("cluster_updateStatus");
                if (_loadPage_enable) {
                    _loadPage_enable = false;
                    vmenu.reloadCurrent();
                    return;
                }
            }
            $("#shardsInfo").html(infolist.join("<br>"));
            var status = '<span class="layui-badge layui-bg-green">正常</span>';
            if (d.status == "yellow") {
                status = '<span class="layui-badge layui-bg-orange">分片缺失</span>';
            } else if (d.status == "red") {
                status = '<span class="layui-badge">异常</span>';
            }
            $("#runStatus").html(status);
        })
    }
    updateStatus();
    vtools.addInterval("cluster-updateStatus", updateStatus, 10);

    function drawTable(c, l, routing_table_indices) {
        var start = c * l;
        var max = c * l + l;
        //清空表格内容
        for (var i in _routing_nodes_ids) {
            for (var i2 = 0; i2 < l; i2++) {
                $("#" + _routing_nodes_ids[i] + i2).html("");
            }
        }
        //填充表格内容
        for (var i = start; i < max; i++) {
            var id = i % l;
            if (i < routing_table_indices.length) {
                var index = routing_table_indices[i];
                var title = [];
                var indexStats = _indices_stats[index.name];
                var diskStat = undefined == indexStats ? "0 KB" : vtools.convertBytes(indexStats.total.store.size_in_bytes);
                var docsStat = undefined == indexStats ? "0" : (indexStats.primaries ? indexStats.primaries.docs.count : '0');
                title.push("<strong>" + index.name + "</strong><br>");
                title.push("<span style='font-size: 12px;'>");
                title.push("占用磁盘: " + diskStat);
                title.push("<br>数据条数: " + docsStat);
                title.push("</span><br>");
                title.push('<a href="#" style="float: left;">信息</a>');
                title.push("<ul class='index-info'>");
                title.push('<li data="' + i + '#state">查看状态</li>');
                title.push('<li data="' + i + '#mappings">查看模板</li>');
                title.push('<li data="' + i + '#aliases">查看别名</li>');
                title.push("<br>");
                title.push('<li>关闭</li>');
                title.push("</ul>");
                title.push(" | <a href='#'>操作</a>");
                title.push("<ul class='index-action'>");
                title.push('<li data="' + i + '#refresh">刷新索引</li>');
                title.push('<li data="' + i + '#close">关闭索引</li>');
                title.push('<li data="' + i + '#delete">删除索引</li>');
                title.push('<li data="' + i + '#removeAlias">清空别名</li>');
                title.push("<br>");
                title.push('<li>关闭</li>');
                title.push("</ul>");
                $("#index" + id).html(title.join(""));
                $("#shardsUA" + id).html("");
                for (var s in index.data.shards) {
                    for (var s1 in index.data.shards[s]) {
                        var b = index.data.shards[s][s1];
                        if (b.state == "UNASSIGNED") {
                            if (b.primary) {
                                $("#shardsUA" + id).append('<div class="unassigned-block primary-shard">' + s + '</div>');
                            } else {
                                $("#shardsUA" + id).append('<div class="unassigned-block">' + s + '</div>');
                            }
                        } else {
                            if (b.primary) {
                                $("#" + b.node + id).append('<div class="assigned-block primary-shard" data="' + i + '#' + s + '#' + s1 + '">' + s + '</div>');
                            } else {
                                $("#" + b.node + id).append('<div class="assigned-block" data="' + i + '#' + s + '#' + s1 + '">' + s + '</div>');
                            }
                        }
                    }
                }
            } else {
                $("#index" + id).text("");
                $("#shardsUA" + id).text("");
            }
        }
        $(".assigned-block").click(function () {
            var pos = $(this).attr("data");
            var act = function (id) {
                var a = pos.split("#");
                $("#" + id).jsonViewer(_routing_table_indices_filtered[a[0]].data.shards[a[1]][a[2]]);
            }
            vmodule.showModule(act);
        })
        //索引信息查看
        $("ul.index-info li").click(function () {
            var pos = $(this).attr("data");
            if ("" == pos || undefined == pos) {
                return;
            }
            var act = function (id) {
                var a = pos.split("#");
                var name = _routing_table_indices_filtered[a[0]].name;
                var act = a[1];
                if ("state" == act) {
                    $("#" + id).jsonViewer(_indices_stats[name]);
                } else {
                    $("#" + id).jsonViewer(_indices_metadata.indices[name][act]);
                }
            }
            vmodule.showModule(act);
        })
        //索引操作
        $("ul.index-action li").click(function () {
            var pos = $(this).attr("data");
            if ("" == pos || undefined == pos) {
                return;
            }
            var a = pos.split("#");
            var name = _routing_table_indices_filtered[a[0]].name;
            var act = a[1];
            if ("close" == act) {
                vmodule.rConfirm2({
                    msg: "确定关闭索引?"
                    , url: "/" + name + "/_close"
                    , method: "POST"
                })
            } else if ("delete" == act) {
                vmodule.rConfirm2({
                    msg: "确定删除索引?"
                    , url: "/" + name
                    , method: "DELETE"
                })
            } else if ("refresh" == act) {
                vhttp.ajax("/" + name + "/_refresh", "POST", function (d) {
                    vmenu.reloadCurrent();
                })
            } else if ("removeAlias" == act) {
                vmodule.rConfirm2({
                    msg: "确定清空别名?"
                    , url: "/_aliases"
                    , method: "POST"
                    , data: JSON.stringify({
                        "actions": [
                            { "remove": { "index": name, "alias": "*" } }
                        ]
                    })
                })
            }
        })
        $("th[id*='index'] a").click(function () {
            $(this).parent().children("ul").hide();
            $(this).parent().siblings().children("ul").hide();
            $(this).next().show();
        })
        $("th[id*='index'] ul").each(function () {
            $(this).children("li:last").click(function () {
                $(this).parent().hide();
            })
        })
    }

    vhttp.ajax("/_cluster/state", "GET", function (d) {
        _routing_nodes = d.routing_nodes;
        _indices_metadata = d.metadata;

        for (var i in d.routing_table.indices) {
            _routing_table_indices.push({ "name": i, "data": d.routing_table.indices[i] });
        }
        _routing_table_indices_filtered = _routing_table_indices;
        $("#indexCount").text(_routing_table_indices.length);

        var dataBody = [];
        var master = d.master_node;
        for (var i in d.nodes) {
            _routing_nodes_ids.push(i);
            var index = d.nodes[i];
            dataBody.push('<tr>');
            dataBody.push('<td id=' + i + '>');
            if (i == master) {
                dataBody.push('<i class="layui-icon layui-icon-rate-solid"></i>&nbsp;'
                    + index.name
                    + '(' + index.transport_address + ')<br>');
            } else {
                dataBody.push(index.name + ' (' + index.transport_address + ')<br>');
            }
            dataBody.push('CPU: <span id="' + i + '_c_t"></span><br><div class="layui-progress" lay-filter="' + i + '_c" lay-showPercent="true"><div class="layui-progress-bar" lay-percent="0%"></div></div>');
            dataBody.push('内存: <span id="' + i + '_m_t"></span><br><div class="layui-progress" lay-filter="' + i + '_m" lay-showPercent="true"><div class="layui-progress-bar" lay-percent="0%"></div></div>');
            dataBody.push('磁盘: <span id="' + i + '_d_t"></span><br><div class="layui-progress" lay-filter="' + i + '_d" lay-showPercent="true"><div class="layui-progress-bar" lay-percent="0%"></div></div>');
            dataBody.push('</td>');
            for (var num = 0; num < PAGE_LIMIT; num++) {
                dataBody.push('<td id="' + i + "" + num + '">' + '</td>');
            }
            dataBody.push('</tr>');
        }
        $("#dataBody").append(dataBody.join(""));

        var nodeStats = function () {
            var changeColor = function (id, per) {
                var pmark = $('div[lay-filter="' + id + '"] div');
                if (per > 80) {
                    pmark.addClass("layui-bg-red");
                } else {
                    pmark.removeClass("layui-bg-red");
                }
            }
            vhttp.ajax("/_nodes/stats/os,fs,process", "GET", {}, function (d) {
                for (var i in d.nodes) {
                    $("#" + i + "_m_t").text(vtools.convertBytes(d.nodes[i].os.mem.used_in_bytes) + "/" + vtools.convertBytes(d.nodes[i].os.mem.total_in_bytes));
                    $("#" + i + "_d_t").text(vtools.convertBytes(d.nodes[i].fs.total.total_in_bytes - d.nodes[i].fs.total.available_in_bytes) + "/" + vtools.convertBytes(d.nodes[i].fs.total.total_in_bytes));
                    $("#" + i + "_c_t").text(d.nodes[i].process.cpu.percent + " %");
                    var dp = Math.floor((100 - (d.nodes[i].fs.total.available_in_bytes / d.nodes[i].fs.total.total_in_bytes) * 100));
                    var mp = Math.floor((d.nodes[i].os.mem.used_in_bytes / d.nodes[i].os.mem.total_in_bytes) * 100);
                    var cp = d.nodes[i].process.cpu.percent;
                    changeColor(i + "_d", dp);
                    changeColor(i + "_m", mp);
                    changeColor(i + "_c", cp);
                    element.progress(i + "_d", dp + "%");
                    element.progress(i + "_m", mp + "%");
                    element.progress(i + "_c", cp + "%");
                }
            }, function (d) {
                console.log(d);
            })
        }

        nodeStats();
        vtools.addInterval("cluster-nodeStats", nodeStats, 10);

        vhttp.ajax("/_stats", "GET", function (d) {
            _indices_stats = d.indices;
            $("#shardCount").text(d._shards.total);
            $("#docCount").text(d._all.primaries.docs.count);
            $("#diskSize").text(vtools.convertBytes(d._all.total.store.size_in_bytes));
            //生成分页
            laypage.render({
                elem: 'dataPage'
                , limit: PAGE_LIMIT
                , count: _routing_table_indices_filtered.length
                , layout: ['prev', 'page', 'next', 'count']
                , jump: function (obj, first) {
                    drawTable(obj.curr - 1, obj.limit, _routing_table_indices_filtered);
                }
            })
        })
    })

    $("#search").keyup(function (e) {
        var text = $(this).val();
        _routing_table_indices_filtered = _routing_table_indices;
        if ("" != text) {
            _routing_table_indices_filtered = [];
            for (var i in _routing_table_indices) {
                if (_routing_table_indices[i].name.indexOf(text) != -1) {
                    _routing_table_indices_filtered.push(_routing_table_indices[i]);
                }
            }
        }
        laypage.render({
            elem: 'dataPage'
            , limit: PAGE_LIMIT
            , count: _routing_table_indices_filtered.length
            , layout: ['prev', 'page', 'next', 'count']
            , jump: function (obj, first) {
                drawTable(obj.curr - 1, obj.limit, _routing_table_indices_filtered)
            }
        })
    })
})