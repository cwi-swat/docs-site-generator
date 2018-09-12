node {
    try {
        def mvnHome = tool 'M3'
        env.JAVA_HOME="${tool 'jdk-oracle-8'}"
        env.PATH="${env.JAVA_HOME}/bin:${mvnHome}/bin:${env.PATH}"

        env.NEXUS_ROOT="http://nexus.usethesource.io"

        stage('Clone & Reset'){
            checkout scm
            sh "rm -rf site && mkdir -p site/www"
        }

        stage("Download & Unpack") {
            // we use the stable link, as the update site script contains a fixed version identifier, the lastest nexus release is not the same as the stable release.
            // downside is that this Jenkinsfile has to be re-run after a new stable release, as it takes a minute for the update site to process it
            sh 'curl -L "https://update.rascal-mpl.org/console/rascal-shell-stable.jar" > stable.jar'
            // for the unstable, we do use nexus, to avoid always being one commit behind the unstable release on the slower-to-update update site
            sh 'curl -L "$NEXUS_ROOT/service/local/artifact/maven/content?g=org.rascalmpl&a=rascal&r=snapshots&v=LATEST" > unstable.jar'

            sh 'jar xf stable.jar boot/courses   && mv boot/courses site/www/stable '
            sh 'jar xf unstable.jar boot/courses && mv boot/courses site/www/unstable '

            sh 'rm -r *.jar boot/'
        }

        stage("Fixup") {
            if (!fileExists('site/www/stable/search-results.html')) {
                // as long as stable doesn't contain the new search api, fix it
                sh "find site/www/stable -name *.html -print0 | xargs -0 sed -i 's,action=\"/Search\",action=\"/search-results.html\" method=\"get\",g'"
                sh "cp site/www/{un,}stable/search-results.html"
            }

            sh "find site/www/stable -name *.html -print0   | xargs -0 sed -i 's,\\(src\\|href\\|action\\)=\"/,\\1=\"/stable/,g'"
            sh "find site/www/unstable -name *.html -print0 | xargs -0 sed -i 's,\\(src\\|href\\|action\\)=\"/,\\1=\"/unstable/,g'"
            // fix location of favicon file
            sh "cp site/www/unstable/favicon.ico site/www"
            sh "cp site/www/unstable/favicon.ico site/www/stable/fonts/"
            sh "cp site/www/unstable/favicon.ico site/www/unstable/fonts/"

            // fix incorrect font-awesome in header
            sh "find site/www -name *.html -print0 | xargs -0 sed -i 's,<link rel=\"stylesheet\" href=\"./font-awesome.css\">,,'"
        }

        stage("Compress") {
            // compress the large HTML pages with some kind of minification?

            // precompress all files so that nginx is faster in serving them
            sh "find site/www -type f -print0 | xargs -0 -n 1 -I{} 7za a -mx9 {}.gz {}"
        }

        withMaven(maven: 'M3', jdk: 'jdk-oracle-8', options: [artifactsPublisher(disabled: true), junitPublisher(disabled: true)] ) {
            stage("Merge indexes") {
                sh 'mkdir site/search'
                sh 'mvn clean compile'
                sh 'mvn exec:java -Dexec.args="site/search/stable site/www/stable/"'
                sh 'mvn exec:java -Dexec.args="site/search/unstable site/www/unstable/"'
            }

            stage('Package') {
                sh 'tar czf full-site.tar.gz site/'
            }
            
            stage('Deploy') {
                sh 'mvn deploy:deploy-file -DgroupId=org.rascalmpl -DartifactId=docs-tutor-site -Dversion=1.0.0-SNAPSHOT -DgeneratePom=false -Dpackaging=tar.gz -Dfile=full-site.tar.gz -DrepositoryId=usethesource-snapshots -Durl=$NEXUS_ROOT/content/repositories/snapshots/'
            }
        }
    } catch(e) {
        slackSend (color: '#d9534f', message: "FAILED: <${env.BUILD_URL}|${env.JOB_NAME} [${env.BUILD_NUMBER}]>")
        throw e
    }
}