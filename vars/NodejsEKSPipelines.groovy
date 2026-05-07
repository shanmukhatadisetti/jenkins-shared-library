def call(Map configMap){
    pipeline {
        agent {
            label 'agent-1'
        }
        environment {
            appVersion = ""
            REGION = "us-east-1"
            ACC_ID = "430774481266"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
            GITHUB_REPO = "shanmukhatadisetti-devops/catalogue-ci"

        }
        options{
            timeout(time: 10, unit: 'MINUTES')
            disableConcurrentBuilds()
        }
        stages {
            stage('read Package.Json'){
                steps{
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "The current version is: ${appVersion}"
                    }
                }
            }
            stage('Install Dependancies'){
                steps{
                    script{
                        sh """
                            npm install
                        """
                    }
                }
            }        
            /* stage('Check Dependabot Alerts') {
                steps {
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {

                        sh '''
                        echo "Checking Dependabot alerts..."

                        RESPONSE=$(curl -s -L \
                        -H "Accept: application/vnd.github+json" \
                        -H "Authorization: Bearer ${GITHUB_TOKEN}" \
                        -H "X-GitHub-Api-Version: 2022-11-28" \
                        https://api.github.com/repos/${GITHUB_REPO}/dependabot/alerts)

                        echo "$RESPONSE" > dependabot-alerts.json

                        COUNT=$(echo "$RESPONSE" | jq '[.[] | select(.state=="open" and (.security_vulnerability.severity=="high" or .security_vulnerability.severity=="critical"))] | length')

                        echo "High/Critical open alerts count: $COUNT"

                        if [ "$COUNT" -gt 0 ]; then
                            echo "Build failed due to HIGH/CRITICAL Dependabot alerts"
                            exit 1
                        else
                            echo "No blocking Dependabot alerts found"
                        fi
                        '''
                    }
                }
            } */
            stage('Docker Build'){
                steps{
                    script{
                            withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                                sh"""
                                    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com
                                    docker build -t ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                                    docker push ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                                """    
                            }
                    }
                }
            }
            /* stage('Trivy Scanning'){
                steps{
                    script{
                        sh"""
                            trivy image --pkg-types os --scanners vuln --severity HIGH,CRITICAL,MEDIUM --exit-code 1 --ignore-unfixed ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                        """
                    }
                }
            } */
        }
    } 
}