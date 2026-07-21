@echo off
echo Starting the 4 Core PEB Services...

start "api-gateway" java -Xmx256m -Dspring.profiles.active=local -jar api-gateway\build\libs\api-gateway-0.1.0-SNAPSHOT.jar
start "identity-service" java -Xmx256m -Dspring.profiles.active=local -jar identity-service\build\libs\identity-service-0.1.0-SNAPSHOT.jar
start "business-service" cmd /c "java -Xmx256m -Dspring.profiles.active=local -jar business-service\build\libs\business-service-0.1.0-SNAPSHOT.jar > business.log 2>&1"
start "finance-service" cmd /c "java -Xmx256m -Dspring.profiles.active=local -jar finance-service\build\libs\finance-service-0.1.0-SNAPSHOT.jar > finance.log 2>&1"

echo Core services started!
