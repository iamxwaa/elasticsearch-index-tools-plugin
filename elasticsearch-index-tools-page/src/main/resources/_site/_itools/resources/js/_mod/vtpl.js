layui.define(['laytpl', 'vhttp'], function (exports) {
    var laytpl = layui.laytpl;
    var vhttp = layui.vhttp;

    var obj = {
        /**
         * 模板渲染
         * @param {*} tplId 
         * @param {*} data 
         * @param {*} viewId 
         */
        render: function (tplId, data, viewId) {
            var tpl = tplId.innerHTML;
            var view = document.getElementById(viewId);
            laytpl(tpl).render(data, function (html) {
                view.innerHTML = html;
            });
        },
        /**
         * 模板渲染
         * @param {*} obj 
         */
        render2: function (obj) {
            var tpl = (typeof obj.tpl) == "string" ? obj.tpl : obj.tpl.innerHTML;
            var view = document.getElementById(obj.vid);
            laytpl(tpl).render(obj.data, function (html) {
                view.innerHTML = html;
            });
        },
        /**
         * 将模板内容渲染为string返回
         * @param {*} obj 
         */
        render3: function (obj) {
            var tpl = (typeof obj.tpl) == "string" ? obj.tpl : obj.tpl.innerHTML;
            return laytpl(tpl).render(obj.data);
        },
        /**
         * ajax请求指定地址,将返回数据渲染到指定模板
         * @param {*} obj 
         */
        ajax: function (obj) {
            var that = this;
            vhttp.ajax(obj.url, obj.method ? obj.method : "GET", function (data) {
                that.render(obj.tpl, data, obj.vid)
            })
        }
    }

    exports('vtpl', obj);
})