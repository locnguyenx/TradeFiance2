# The Tools Application Entity Tools

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

# Entity Tools

  
* [Data Edit](#data-edit)
* [Data Export](#data-export)
* [Data Import](#data-import)
* [SQL Runner](#sql-runner)
* [Speed Test](#speed-test)
* [Query Stats](#query-stats)
* [Data Snapshots](#data-snapshots)

## Data Edit

The data edit screens are somewhat similar to the Auto Screens, but without the tab sets and instead on the entity edit screen a list of related entities with a link to find records related to the current record, as you can see here. These screens still have their uses but are mostly superseded by the Auto Screens.

## Data Export

This screen is used to export entity data in one or more entity XML files, or out to the browser. Select one or more entity names, from/thru dates to filter by the **lastUpdatedStamp**, the output path or filename (leave empty for Out to Browser), an optional Map in Groovy syntax to filter by (filter fields only applied to entities with matching field names, otherwise ignored), and optional comma-separated order by field names (also only applies to entities with matching field names).

## Data Import

Use this screen to import data from entity XML, JSON  or CSV text. There are 3 options for the text itself: comma-separated data types (matching the _entity-facade-xml.type_ attribute), a resource location that can be a local filename or any location supported by the Resource Facade, or text pasted right into the browser in a textarea. Dummy FKs checks each record’s foreign keys and if a record doesn’t exist adds one with only PK fields populated. Use Try Insert is meant for data that is expected to not exist and instead of querying each record to see if it does it just tries an insert and if that fails does an update (slower for lots of updates). Check Only doesn’t actually load the data and instead checks each record and reports the differences.

## SQL Runner

Use this screen to run arbitrary SQL statements against the database for a given entity group and view the results.

## Speed Test

This screen runs a series of cache and entity operations to report timing results. It is most useful to see comparative performance between different databases and server configurations. The screen accepts a baseCalls parameter which defaults to 1000 (as seen below). Note that this screen shot uses the default configuration with the "nosql" entity group in the Derby database along with all the others. When using OrientDB or some other NoSQL datasource you’ll see fairly different results.

## Query Stats

This screen is used to show the statistics for queries run since server start. All times are in microseconds..

## Data Snapshots

This Screen used to export, import and upload data snapshots. It is also used for create and drop Foreign keys.

The recommended approach to load a full database snapshot (as of Moqui 3) is:

```
`# start with a fresh local build or clean local H2 and ElasticSearch data
$ gradle cleanDb
# load file by location in raw mode - location is absolute path or relative to runtime directory - creates tables but no FKs, no feed to ElasticSearch
$ java -jar moqui.war load raw location=db/snapshot/MoquiSnapshot20200223-1258.zip
# start moqui normally, will see ElasticSearch indexes don't exist so triggers the feeds with indexOnStartEmpty="Y", OOTB just MantleSearch DataFeed in mantle-usl
$ java -jar moqui.war
# Foreign Keys are still missing so go to /vapps/tools/Entity/DataSnapshot and run 'Create FKs'
`
```