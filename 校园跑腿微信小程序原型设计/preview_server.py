#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
简单的HTTP服务器，用于预览HTML文件
支持中文文件名
"""

import http.server
import socketserver
import os
import urllib.parse

class ChineseHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def translate_path(self, path):
        # 解码URL中的中文路径
        path = urllib.parse.unquote(path)
        return super().translate_path(path)
    
    def log_message(self, format, *args):
        # 简化日志输出
        print(f"{self.client_address[0]} - {format % args}")

def run_server(port=8080):
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    with socketserver.TCPServer(("", port), ChineseHTTPRequestHandler) as httpd:
        print(f"服务器启动在 http://localhost:{port}")
        print(f"工作目录: {os.getcwd()}")
        print("按 Ctrl+C 停止服务器")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n服务器已停止")

if __name__ == "__main__":
    run_server()