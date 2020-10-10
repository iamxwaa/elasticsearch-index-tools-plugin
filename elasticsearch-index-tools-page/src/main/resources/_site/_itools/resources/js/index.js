layui.use(['vmenu', 'vhttp'], function () {
    var sidemenu = layui.vmenu;
    var vhttp = layui.vhttp;
    vhttp.ajax("_itools/version", "GET", function (d) {
        var side = sidemenu.getSideMenu(d);
        sidemenu.renderMenu('sideMenu', side);
    })
});