# Jira Cards Printer

This is very simplistic plugin that let's you generate PDF file with cards that you might print, cut and place on your scrum/kanban board.

You can navigate to url `<your JIRA base url>/plugins/servlet/yakuza/cardsprinter`, so if you access jira by URL: `https://example.com/jira`, then type: `https://example.com/jira/plugins/servlet/yakuza/cardsprinter`. Then type JQL query, press Enter and below all issues, there will be "Print" button.

Otherwise you can directly perform GET request on resource: `<JIRA base url>/plugins/servlet/yakuza/cardsprintpreview?jqlQuery==<urlencoded JQL query>` which in turn will return application/pdf with issues matching given query.

To generate PDF files [PDFBox](https://pdfbox.apache.org/) is used.
