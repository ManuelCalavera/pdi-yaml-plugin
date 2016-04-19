## Installation
Copy the folder `SeibelsYAMLInput` from the `plugin` directory into kettle's plugin directory. This directory is by default `$PENTAHO_HOME/plugins/steps`. When you restart Spoon the plugin will be available in transformations under a new folder titled `Seibels Plugins`.

## About
The purpose of writing a custom YAML step is to add support for multiple YAML patterns using the SnakeYAML library. The packaged YAML step is not well documented and doesn't supply guidelines for supported formats. An example of one of the intended formats that the plugin can parse is as follows:

```
tables:
  - {schema: dbo, table: Address, sql: Address.sql}
  - {schema: dbo, table: Item, sql: Item.sql}
  - {schema: dw, table: Status, sql: }

lookups:
  - {name: Product, sql: Product.sql}
  - {name: Form, sql: Form.sql}
  - {name: Coverage, sql: Coverage.sql}
```

## Use
To use the plugin start by dragging the created step from the `Seibels Plugins` folder into the transformation. Set the path of the YAML file in the box `File Path`, this path can be a variable. At the moment the step does not process row input and all incoming rows will be dropped. The step is intended to generate rows only.

When ran the YAML will be parsed and every YAML entry will be sent to kettle as a row. Every unique key will then be set as a field. Using the above example the output would be:
```
tables-schema: dbo  tables-table: Address  tables-sql: Address.sql  lookups-name: null  lookups-sql: null
tables-schema: dbo  tables-table: Item     tables-sql: Item.sql     lookups-name: null  lookups-sql: null
tables-schema: dw   tables-table: Status   tables-sql: null         lookups-name: null  lookups-sql: null

tables-schema: null  tables-table: null  tables-sql: null  lookups-name: Product   lookups-sql: Product.sql
tables-schema: null  tables-table: null  tables-sql: null  lookups-name: Form      lookups-sql: Form.sql
tables-schema: null  tables-table: null  tables-sql: null  lookups-name: Coverage  lookups-sql: Coverage.sql
```

## Warnings
The step is still in it's early stages. Some YAML patterns may not produce correct rows. Use at your own risk.