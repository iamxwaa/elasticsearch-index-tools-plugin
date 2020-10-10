layui.define(['layer', 'vhttp', 'vmenu'], function (exports) {
    var layer = layui.layer;
    var vhttp = layui.vhttp;
    var vmenu = layui.vmenu;

    var obj = {
        /**
         * 调用act方法生成弹出框
         * @param {function} act 
         */
        showModule: function (act) {
            layer.open({
                type: 1
                , area: '800px'
                , offset: 't'
                , id: 'shardInfo'
                , content: '<div id="html_content" style="max-height: 600px;"></div>'
                , btn: '关闭'
                , btnAlign: 'r'
                , shade: 0.6
                , yes: function () {
                    layer.closeAll();
                }
                , success: function () { act("html_content") }
            })
        },
        /**
         * 确认弹框
         * @param {string} message 
         * @param {function} yes 
         */
        showConfirm: function (message, yes) {
            layer.confirm(message, {
                btn: ['确定', '关闭']
            }, function (index, layero) {
                yes(layer);
            }, function (index) {
                layer.closeAll();
            });
        },
        /**
         * 显示确认弹框,确定后ajax请求指定url,并执行回调
         * @param {object} obj 
         */
        rConfirm: function (obj) {
            layer.confirm(obj.msg, {
                btn: ['确定', '关闭']
            }, function (index, layero) {
                if (undefined != obj.data) {
                    vhttp.ajax(obj.url, obj.method, obj.data, function (data) {
                        obj.callback(data);
                    })
                } else {
                    vhttp.ajax(obj.url, obj.method, function (data) {
                        obj.callback(data);
                    })
                }
            }, function (index) {
                layer.closeAll();
            });
        },
        /**
         * 显示确认弹框,确定后ajax请求指定url,并刷新当前页面
         * @param {object} obj 
         */
        rConfirm2: function (obj) {
            layer.confirm(obj.msg, {
                btn: ['确定', '关闭']
            }, function (index, layero) {
                if (undefined != obj.data) {
                    vhttp.ajax(obj.url, obj.method, obj.data, function (data) {
                        vmenu.reloadCurrent();
                        layer.closeAll();
                    })
                } else {
                    vhttp.ajax(obj.url, obj.method, function (data) {
                        vmenu.reloadCurrent();
                        layer.closeAll();
                    })
                }
            }, function (index) {
                layer.closeAll();
            });
        }
    }

    exports('vmodule', obj);
})