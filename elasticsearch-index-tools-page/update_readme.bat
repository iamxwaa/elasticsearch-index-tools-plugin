@echo off
rem 修改README.md文件后,执行此文件更新readme.html页面
..\mdtrans.exe -path src\main\resources\_site\_itools\resources\page\README.md -target html -name readme.html