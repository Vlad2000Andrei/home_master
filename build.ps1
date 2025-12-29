./gradlew bootJar

Remove-Item ./build/libs/*plain.jar

docker buildx build --platform linux/arm64 -t home-master:pi .