#!/usr/bin/env groovy

/**
 * Publishes the JaCoCo HTML report and creates a coverage trend plot.
 *
 * Only the code coverage of the last build result is kept.
 * The performance is great.
 *
 * @param reportDir path to the directory where the JaCoCo XML report is kept.
 *					The HTML report needs to be in this directory or an 'html'
 *					subdirectory.
 * @param reportXml name of the JaCoCo XML report.
 * @param reportName name of the report, 'Coverage: ' is prefixed automatically.
 *
 * @note For this to work you need both the HTML and XML report activated.
 * @note Reasons why to avoid existing plugins:
 * * The Cobertura plugin takes roughly 1:30 minutes to collect the coverage for Catroid.
 * * The Code Coverage API plugin takes more than 10 minutes to collect the coverage for Catroid.
 * * The reason not to use the existing JaCoCo plugin is that it only supports the binary
 *   ec files, which means that the JaCoCo version embeded in the Jenkins plugin needs to
 *   match the JaCoCo version used to create the ec files. Furthermore there were other
 *   paint points discussed at https://confluence.catrob.at/x/2YE7Ag
 */
void call(String reportDir, String reportXml, String reportName)
{
    String reportXmlFile = "$reportDir/$reportXml"
    String reportHtmlDir = retrieveJacocoHtmlDir(reportDir)

    publishHTML([reportDir: reportHtmlDir, reportFiles: 'index.html', reportName: "Coverage: $reportName",
                 allowMissing: true, keepAll: false])

    String csvFile = "coverage_${reportName}.csv"
    writeCoverageCsvFile(reportXmlFile, csvFile)
    plot(csvFileName: "plot_coverage_${reportName}.csv", csvSeries: [[file: csvFile]],
         group: 'Coverage Trend', numBuilds: '30', style: 'lineSimple', title: reportName,
         yaxis: 'Coverage [%]', yaxisMinimum: '0', yaxisMaximum: '100')
}

String retrieveJacocoHtmlDir(String reportDir) {
    if (fileExists("$reportDir/index.html")) {
        return reportDir
    } else {
        return "$reportDir/html"
    }
}

void writeCoverageCsvFile(String reportXmlFile, String csvFile) {
    String source = '''#!/usr/bin/env python2
import sys
import xml.etree.ElementTree as ET

def write_coverage_csv(report_file, csv_file):
    tree = ET.parse(report_file)
    root = tree.getroot()

    coverage_types = ('INSTRUCTION', 'BRANCH', 'LINE', 'METHOD', 'CLASS')

    names = []
    values = []
    for counter in root.findall('counter'):
        coverage_type = counter.get('type')
        if coverage_type in coverage_types:
            names.append(coverage_type.lower().capitalize())
            covered = float(counter.get('covered'))
            missed = float(counter.get('missed'))
            values.append(str(100 * (covered / (covered + missed))))

    names, values = zip(*(sorted(zip(names, values))))

    with open(csv_file, 'w') as f:
        f.write(','.join(names) + '\\n' + ','.join(values))

write_coverage_csv(sys.argv[1], sys.argv[2])
'''
    writeFile file: 'retrieve_total_coverage_csv.py', text: source
    sh 'chmod a+x ./retrieve_total_coverage_csv.py'
    sh "./retrieve_total_coverage_csv.py '$reportXmlFile' '$csvFile'"
}
