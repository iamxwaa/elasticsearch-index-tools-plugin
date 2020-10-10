layui.define(function (exports) {
    var _host = window.location.origin;

    var obj = {
        /**
         * 获取当前主机地址: http://<ip>:<port>
         */
        getHost: function () {
            return _host;
        },
        /**
         * ajax请求
         * @param {string} url 
         * @param {string} type 
         * @param {*} dataBody 
         * @param {function} func 
         * @param {function} errorfunc 
         */
        ajax: function (url, type, dataBody, func, errorfunc) {
            if (typeof dataBody === "function") {
                func = dataBody;
                dataBody = {};
            }
            if ("{}" == dataBody && (type == "GET" || type == "DELETE")) {
                dataBody = "";
            }
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            $.ajax({
                "url": _host + url,
                "type": type,
                "crossDomain": true,
                "headers": {},
                "data": dataBody,
                "contentType": "application/json;charset=UTF-8",
                "success": function (data) {
                    if (undefined != func) {
                        func(data);
                    } else {
                        console.log(data);
                    }
                },
                "error": function (a, b, c) {
                    if (undefined == errorfunc) {
                        alert(a.responseText);
                    } else {
                        errorfunc(a, b, c);
                    }
                }
            })
        }
    }

    exports('vhttp', obj);
})