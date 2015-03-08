# AndroidStudioDbModelGenerator

Plugin for IntelliJ IDEA to generate Android compatible database model from class with fields. Plugin is compatible with AndroidStudio.

It Generates:

* Android Parcelable implementation - based on https://github.com/mcharmas/android-parcelable-intellij-plugin
* Database field name constants for SQL table. New fields are added in-order with previously added fields. Thus if field is already added and is defined in a same way, it is skipped. 
* CreateTable field with SQL create table statement.
* Full projection field - all fields in DB model, for ContentProvider queries.
* CreateFromCursor implementation. Initializes DB model object with Android Cursor.
* GetContentValues implementation - returns ContentValues object initialized with current object data, used for insert/update requests.
