layui.use(['table', 'vhttp', 'vmodule', 'vtools', 'vtpl'], function () {
    var table = layui.table;
    var vhttp = layui.vhttp;
    var vmodule = layui.vmodule;
    var vtools = layui.vtools;
    var vtpl = layui.vtpl;

    table.render({
        elem: '#nodeTable'
        , data: []
        , page: false
        , limit: 5000
        , cols: [[
            { field: 'node', title: '节点', width: 200, style: 'node-table-info4', fixed: 'left' }
            , { field: 'roles', title: '属性', width: 200 }
            , { field: 'cpu', title: 'CPU %', width: 180 }
            , { field: 'memory', title: '内存 %', width: 220 }
            , { field: 'disk', title: '磁盘 %', width: 220 }
            , { field: 'startTime', title: '启动时间', width: 180 }
            , { field: 'id', hide: true }
            , { fixed: 'right', align: 'center', toolbar: '#nodeTableBar' }
        ]]
    });

    var rowNode = "<strong>名称 : </strong>{{ d.name }}<br><strong>HOST : </strong>{{ d.host }}<br><strong>IP : </strong>{{ d.ip }}";
    var rowRoles = "<strong>-</strong>";
    var rowCpu = "<span class='node-table-info'>{{ d.process.cpu.percent }}</span>";
    var rowMemory = "<div>"
        + "<span class='node-table-info1'>{{ d.os.mem.used_percent  }}</span>"
        + "<span class='node-table-info2'>"
        + "<div>已用 : {{ layui.vtools.convertBytes(d.os.mem.used_in_bytes) }}</div>"
        + "<div>总共 : {{ layui.vtools.convertBytes(d.os.mem.total_in_bytes) }}</div>"
        + "</span>"
        + "</div>";
    var rowDisk = "<div>"
        + "<span class='node-table-info1'> {{ layui.vtools.percent(d.fs.total.available_in_bytes,d.fs.total.total_in_bytes) }} </span>"
        + "<span class='node-table-info2'>"
        + "<div>已用 : {{ layui.vtools.convertBytes(d.fs.total.total_in_bytes - d.fs.total.available_in_bytes) }}</div>"
        + "<div>总共 : {{ layui.vtools.convertBytes(d.fs.total.total_in_bytes) }}</div>"
        + "</span>"
        + "</div>";

    //第一次加载时保存全部host节点信息
    var globalHost;

    var loadTable = function () {
        vhttp.ajax("/_nodes/stats/os,fs,jvm,indices,process", "GET", function (data) {
            var dataBody = [];
            var runningHosts = {};
            function updateState() {
                for (var i in globalHost) {
                    globalHost[i] = 0;
                    var state = runningHosts[i];
                    if (undefined == state) {
                        if (i == "127.0.0.1") {
                            state = runningHosts["localhost"];
                            if (undefined != state) {
                                delete globalHost["127.0.0.1"];
                                globalHost["localhost"] = 1;
                            }
                        } else if (i == "localhost") {
                            state = runningHosts["127.0.0.1"];
                            if (undefined != state) {
                                delete globalHost["localhost"];
                                globalHost["127.0.0.1"] = 1;
                            }
                        }
                    } else {
                        globalHost[i] = 1;
                    }
                }
                for (var i in globalHost) {
                    if (globalHost[i] == 0) {
                        var row = {};
                        row.id = i;
                        runningHosts[node.host] = 1;
                        row.node = vtpl.render3({ tpl: rowNode, data: { name: "-", host: i, ip: "-" } });
                        row.roles = vtpl.render3({ tpl: rowRoles, data: { roles: "-" } });
                        row.cpu = vtpl.render3({ tpl: rowCpu, data: { os: { cpu: { percent: "-" } } } });
                        row.memory = vtpl.render3({ tpl: rowMemory, data: { os: { mem: { used_percent: 0, used_in_bytes: 0, total_in_bytes: 0 } } } });
                        row.disk = vtpl.render3({ tpl: rowDisk, data: { fs: { total: { total_in_bytes: 0, available_in_bytes: 0 } } } });
                        row.startTime = "<strong style='color: #FF5722;'>停止</strong>";
                        dataBody.push(row)
                    }
                }
                table.reload('nodeTable', {
                    data: dataBody
                })
            }
            for (var i in data.nodes) {
                var node = data.nodes[i];
                var row = {};
                row.id = i;
                runningHosts[node.host] = 1;
                row.node = vtpl.render3({ tpl: rowNode, data: node });
                row.roles = vtpl.render3({ tpl: rowRoles, data: node });
                row.cpu = vtpl.render3({ tpl: rowCpu, data: node });
                row.memory = vtpl.render3({ tpl: rowMemory, data: node });
                row.disk = vtpl.render3({ tpl: rowDisk, data: node });
                row.startTime = vtools.getTime(new Date(node.jvm.timestamp - node.jvm.uptime_in_millis));
                dataBody.push(row);
            }

            if (undefined != globalHost) {
                updateState();
                return;
            }

            vhttp.ajax("/_nodes/settings", "GET", function (data) {
                globalHost = {}
                for (var i in data.nodes) {
                    var node = data.nodes[i];
                    var hosts = node.settings.discovery ? node.settings.discovery.zen.ping.unicast.hosts : [];
                    for (var j in hosts) {
                        globalHost[hosts[j]] = 0;
                    }
                }
                updateState();
            })

            table.on('tool(node)', function (obj) {
                var act = function (id) {
                    vhttp.ajax("_nodes/" + obj.data.id + "/stats?human", "GET", function (d) {
                        vtools.renderJson(id, d);
                    })
                }
                if (obj.event === 'detail') {
                    vmodule.showModule(act);
                }
            })
        })
    }

    loadTable();

    vtools.addInterval("node-loadTable", loadTable, 10);
})