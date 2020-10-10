
layui.define(['element', 'vtools', 'vhttp'], function (exports) {
    var element = layui.element;
    var vtools = layui.vtools;
    var vhttp = layui.vhttp;

    //全部页面链接配置
    //命名规则 m<序号>[_<es大版本号>x]
    var m1 = { name: "概览", addr: "#/page/cluster" }
    var m1_2x = { name: "概览", addr: "#/2x/page/cluster" }
    var m2 = { name: "节点状态", addr: "#/page/nodes" }
    var m2_2x = { name: "节点状态", addr: "#/2x/page/nodes" }
    var m3 = { name: "数据", addr: "#/page/data" }
    var m4 = { name: "查询", addr: "#/page/rest" }
    var m5 = { name: "数据仓库", addr: "#/page/snapshot/repository" }
    var m6 = { name: "数据备份", addr: "#/page/snapshot/backup" }
    var m7 = { name: "工具", addr: "#/page/other/tools" }
    var m8 = { name: "模板", addr: "#/page/other/templates" }
    var m8_2x = { name: "模板", addr: "#/2x/page/other/templates" }
    var m8_5x = { name: "模板", addr: "#/5x/page/other/templates" }
    var m9 = { name: "别名", addr: "#/page/other/aliastools" }
    var m10 = { name: "索引预创建", addr: "#/page/other/autocreate" }
    var m11 = { name: "cat apis", addr: "#/page/other/cat" }
    var m12 = { name: "权限", addr: "#/page/right" }
    var m13 = { name: "版本信息", addr: "#/page/info/version" }
    var m14 = { name: "JVM信息", addr: "#/page/info/jvm" }
    var m15 = { name: "组件信息", addr: "#/page/info/plugins" }
    var m16 = { name: "健康信息", addr: "#/page/info/health" }
    var m17 = { name: "测试数据生成", addr: "#/page/other/generatedata" }
    var m18 = { name: "数据统计", addr: "#/page/other/statistic" }
    var m19 = { name: "数据重整", addr: "#/page/other/sortdata" }

    //二级菜单
    var mm1 = { name: "快照管理", children: [m5, m6] }
    var mm2 = { name: "其他", children: [m7, m8, m9, m10, m11,m17,m18,m19] }
    var mm2_2x = { name: "其他", children: [m8_2x, m9, m10, m11,m18,m19] }
    var mm2_5x = { name: "其他", children: [m7, m8_5x, m9, m10, m11,m17,m18,m19] }
    var mm3 = { name: "信息", children: [m13, m14, m15, m16] }

    //版本菜单配置
    //*x2结尾的为不包含权限功能的菜单
    var _menu_ = {
        "default": [m1_2x, m2_2x, m3, m4, mm1, mm2_2x],
        "2x2": [m1_2x, m2_2x, m3, m4, mm1, mm2_2x],
        "5x": [m1, m2, m3, m4, m12, mm1, mm2_5x],
        "5x2": [m1, m2, m3, m4, mm1, mm2_5x],
        "6x": [m1, m2, m3, m4, m12, mm1, mm2, mm3],
        "6x2": [m1, m2, m3, m4, mm1, mm2, mm3],
        "7x": [m1, m2, m3, m4, m12, mm1, mm2],
        "7x2": [m1, m2, m3, m4, mm1, mm2]
    }

    var obj = {
        /**
         * 获取菜单
         * @param {object} info 
         */
        getSideMenu: function (info) {
            var v = info.version;
            if (info.security) {
                return _menu_[v + "x"];
            } else {
                return _menu_[v + "x2"];
            }
        },
        /**
         * 将菜单渲染到指定id位置
         * @param {string} id 
         * @param {object} menu 
         */
        renderMenu: function (id, menu) {
            var sideList = [];
            if (undefined != menu) {
                for (var i = 0; i < menu.length; i++) {
                    var m = menu[i];
                    if (undefined == m) {
                        continue;
                    }
                    if (undefined == m.children) {
                        sideList.push('<li class="layui-nav-item"><a href="' + m.addr + '">' + m.name + '</a></li>');
                    } else {
                        sideList.push('<li class="layui-nav-item">');
                        sideList.push('<a class="" href="javascript:;">' + m.name + '</a>');
                        sideList.push('<dl class="layui-nav-child">');
                        for (var j = 0; j < m.children.length; j++) {
                            var m2 = m.children[j]
                            sideList.push('<dd><a href="' + m2.addr + '">' + m2.name + '</a></dd>');
                        }
                        sideList.push('</dl>');
                        sideList.push('</li>');
                    }
                }
            }
            $("#" + id).html(sideList.join(""));
            element.render('nav');
            //添加跳转事件
            element.on('nav(menu)', function (elem) {
                var h = elem.attr("href");
                if (undefined == h || h.indexOf("#") == -1) {
                    return;
                }
                vtools.emptyInterval();
                var lv1 = $(elem).parents("li").children("a").text();
                var lv2 = elem.text();
                if (lv1 == lv2) {
                    $("#breadcrumbTitle").html('<a lay-href="">' + lv1 + '</a>');
                } else {
                    $("#breadcrumbTitle").html('<a lay-href="">' + lv1 + '</a><a><cite>' + lv2 + '</cite></a>');
                }
                element.render("breadcrumb");
                var url = "/_itools/resources" + h.substring(1) + ".html";
                vhttp.ajax(url, "GET", function (p) {
                    $("#content").html(p);
                })
            });
            var page = window.location.href.split("#");
            if (page.length > 1 && "" != page[1]) {
                this.loadPage(page[1])
                return;
            }
            var onloadPage = $(".layui-nav.layui-nav-tree").children(":first").children("a").attr("href");
            this.loadPage(onloadPage.substring(1));
        },
        /**
         * 加载指定页面
         * @param {string} page 
         */
        loadPage: function (page) {
            var e = $(".layui-nav-item a[href='#" + page + "']");
            var pe = e.parent().parent("dl");
            if (undefined != pe) {
                //展开父级菜单
                pe.prev().click();
            }
            //点击页面菜单
            e.click();
        },
        /**
         * 刷新当前页面
         */
        reloadCurrent: function () {
            $(".layui-this a[href*='#']").click();
        }
    };

    exports('vmenu', obj);
}); 