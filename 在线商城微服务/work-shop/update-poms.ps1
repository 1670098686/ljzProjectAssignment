# 批量为服务模块添加 Spring Boot 插件
$serviceModules = @(
    "mall-gateway",
    "mall-user",
    "mall-product",
    "mall-stock",
    "mall-cart",
    "mall-order",
    "mall-favorite",
    "mall-review"
)

foreach ($module in $serviceModules) {
    $pomFile = "D:\BaiduNetdiskDownload\class\work-shop\$module\pom.xml"
    if (Test-Path $pomFile) {
        $content = Get-Content $pomFile -Raw
        if ($content -notmatch "spring-boot-maven-plugin") {
            $newContent = $content -replace "</dependencies>\s*</project>", "</dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>3.2.0</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>"
            Set-Content $pomFile $newContent
            Write-Host "Updated $module/pom.xml"
        } else {
            Write-Host "$module/pom.xml already has spring-boot-maven-plugin"
        }
    } else {
        Write-Host "$pomFile not found"
    }
}

Write-Host "Done!"