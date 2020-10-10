layui.define(function (exports) {
    var escapeHtmlMap = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
        '/': '&#x2F;',
        '`': '&#x60;',
        '=': '&#x3D;'
    };

    var _KB = 1024 * 1024;
    var _MB = _KB * 1024;
    var _GB = _MB * 1024;
    var _KBD = 1024;
    var _MBD = _KB;
    var _GBD = _MB;
    var _TBD = _GB;

    var interval_cache = {}

    var obj = {
        /**
         * 将es返回的文本数据格式化
         * @param {string} text 
         */
        formatText: function (text) {
            var arr = text.split("\n");
            var draw = [];

            for (var i in arr) {
                draw.push("<p class=\"rsp-text\">" + arr[i].replace(/ /g, "&nbsp;") + "</p>");
            }

            return draw.join('');
        },
        /**
         * 添加周期定时任务
         * @param {string} id id命名规则  <js文件名称>-<方法名称>
         * @param {function} func 
         * @param {number} sec 
         */
        addInterval: function (id, func, sec) {
            if (undefined != interval_cache[id]) {
                this.removeInterval(id);
            }
            console.log("添加周期任务: " + id);
            interval_cache[id] = setInterval(func, sec * 1000);
        },
        /**
         * 移除定时任务
         * @param {string} id 
         */
        removeInterval: function (id) {
            var func = interval_cache[id];
            if (undefined != func) {
                delete interval_cache[id];
                console.log("清除周期任务: " + id);
                clearInterval(func);
            }
        },
        /**
         * 清空全部定时任务
         */
        emptyInterval: function () {
            for (var id in interval_cache) {
                this.removeInterval(id);
            }
        },
        /**
         * 将byte数据转换为kb/mb/gb/tb
         * @param {number} n 
         */
        convertBytes: function (n) {
            if (n < _KB) {
                return (Math.floor(n / _KBD * 100) / 100) + " KB";
            }
            if (n < _MB) {
                return (Math.floor(n / _MBD * 100) / 100) + " MB";
            }
            if (n < _GB) {
                return (Math.floor(n / _GBD * 100) / 100) + " GB";
            }
            return (Math.floor(n / _TBD * 100) / 100) + " TB";
        },
        /**
         * html文本转义
         * @param {string} s 
         */
        escapeHtml: function (s) {
            return String(s).replace(/[&<>"'`=\/]/g, function (a) {
                return escapeHtmlMap[a];
            });
        },
        /**
         * 数据转string
         * @param {*} dataBody 
         */
        toString: function (dataBody) {
            if (typeof dataBody === "object") {
                return this.escapeHtml(JSON.stringify(dataBody));
            }
            return this.escapeHtml(dataBody);
        },
        /**
         * 时间格式化为 yyyy-MM-dd HH:mm:ss
         * @param {Date} date 
         */
        getTime: function (date) {
            a = date ? date : new Date();
            y = a.getFullYear();
            m = a.getMonth() < 9 ? "0" + (a.getMonth() + 1) : (a.getMonth() + 1);
            d = a.getDate() < 10 ? "0" + a.getDate() : a.getDate();
            h = a.getHours() < 10 ? "0" + a.getHours() : a.getHours();
            mm = a.getMinutes() < 10 ? "0" + a.getMinutes() : a.getMinutes();
            s = a.getSeconds() < 10 ? "0" + a.getSeconds() : a.getSeconds();
            return y + "-" + m + "-" + d + " " + h + ":" + mm + ":" + s;
        },
        /**
         * 将json数据转换到指定id位置
         * @param {string} id 
         * @param {*} data 
         * @param {function} func 
         */
        renderJson: function (id, data, func) {
            $("#" + id).jsonViewer(func ? func(data) : data);
        },
        /**
         * 将文本写入到指定id位置
         * @param {string} id 
         * @param {*} data 
         * @param {function} func 
         */
        renderHtml: function (id, data, func) {
            $("#" + id).html(func ? func(data) : data)
        },
        /**
         * 计算数值百分比
         * @param {number} small 
         * @param {number} big 
         */
        percent: function (small, big) {
            if (0 == big) {
                return 0;
            }
            return Math.floor(100 - (small / big) * 100);
        },
        /**
         * 将json展开为数组
         * {a:{b:{c:1}},d:2} => [["a.b.c",1],["d",2]]
         * @param {*}} data 
         */
        spread: function (data) {
            //将json数据展开
            var json = data;
            if (typeof json === "string") {
                json = JSON.parse(json);
            }

            function read(key, value) {
                if (typeof value === "object") {
                    var result = {};
                    for (var i in value) {
                        if (typeof value[i] === "object") {
                            var o = read(i, value[i]);
                            for (var j in o) {
                                result[key + "." + j] = o[j];
                            }
                        } else {
                            result[key + "." + i] = value[i];
                        }
                    }
                    return result;
                } else {
                    return { key: value };
                }
            }

            function toArray(d) {
                var o = read("", d);
                var arr = [];
                for (var i in o) {
                    arr.push([i.substring(1), o[i]]);
                }
                return arr;
            }

            return toArray(json);
        }
    }

    exports('vtools', obj);
})