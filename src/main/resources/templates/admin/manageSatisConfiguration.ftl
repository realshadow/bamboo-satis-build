<html>
    <head>
        <title>Manage Satis Build Configuration</title>
        <meta name="decorator" content="adminpage">
    </head>
<body>
    <h1>Manage Satis Build Configuration</h1>

    [@ww.form submitLabelKey='global.buttons.update' showActionMessages='true' showActionErrors='true']

        [@ww.textfield name="apiUrl" labelKey="satisbuild.apiurl" cssClass="long-field" /]
        [@ww.textfield name="vcsUrl" labelKey="satisbuild.vcsurl" cssClass="long-field" /]

    [/@ww.form]
</body>
</html>