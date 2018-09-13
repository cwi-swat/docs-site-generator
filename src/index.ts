import * as minify from "html-minifier";
import * as UglifyJS from "uglify-js";
import CleanCSS from "clean-css";
import * as process from 'process';
import fs from 'fs';
import klaw from 'klaw';

function main() {

    if (process.argv.length != 3) {
        console.error("Missing directory root argument");
        console.error(process.argv);
        process.exit(1);
        return;
    }
    const rootDir = process.argv[2];

    const defaultOptions: minify.Options = {
        collapseBooleanAttributes : true,
        collapseWhitespace: true,
        conservativeCollapse: true,
        html5: true,
        minifyCSS: true,
        minifyJS: true,
        minifyURLs: false,
        preserveLineBreaks: true,
        removeRedundantAttributes: true,
        sortAttributes: true,
        useShortDoctype: true
    };

    const cleanCss = new CleanCSS({
        compatibility: 'ie9',
        inliner: ['local'],
        root: rootDir + '/css/',
        processImport: false,
        advanced: false,
        aggressiveMerging: false,
        rebase: false,
    });
    
    klaw(rootDir)
        .on("data", (item) => {
            if (/\.(html|js|css)$/.test(item.path) && !/\.min\./.test(item.path)) {
                // we can minify it :)
                fs.readFile(item.path, (err, data) => {
                    if (err) {
                        console.error("Error for: " + item.path);
                        console.error(err);
                    }
                    else {
                        let newContents = "";
                        if (item.path.endsWith(".html")) {
                            newContents= minify.minify(data.toString('utf-8'), defaultOptions);
                        }
                        else if (item.path.endsWith(".js")) {
                            const result = UglifyJS.minify(data.toString('utf-8')); 
                            if (result.error) {
                                console.error("Error for: " + item.path);
                                console.error(result.error);
                                return;
                            }
                            newContents = result.code;
                        }
                        else {
                            const result = cleanCss.minify(data.toString('utf-8'));
                            if (result.errors && result.errors.length > 0) {
                                console.error("Error for: " + item.path);
                                console.error(result.errors);
                                return;
                            }
                            newContents = result.styles;
                        }
                        fs.writeFile(item.path, newContents, 'utf-8', (err) => { if (err) {console.error(err); }});
                    }
                });
            }
        });

}

main();