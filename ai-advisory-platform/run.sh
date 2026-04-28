docker run --rm \
  --network infrastructure_default \
  -v /Users/risko/Data/tools/jenkins_data/report-ai-advisory:/report \
  ai-runner-advisory:v1.0.0 \
  python -m app.main \
    --project-key payment-service \
    --analysis-mode critical_analysis \
    --sonar-url http://sonarqube:9000 \
    --output-file advisory_report.json \
    --report-dir report-17-4-26-payment-service-build-number-5-testing