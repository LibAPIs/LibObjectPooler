# LibObjectPooler

A generic object pool capable of managing and expiring old objects.

## Maven Dependency

Include the library in your project by adding the following dependency to your pom.xml

```
<dependency>
	<groupId>com.mclarkdev.tools</groupId>
	<artifactId>libobjectpooler</artifactId>
	<version>1.5.4</version>
</dependency>
```

## Example

Create a custom `PooledObjectController` which handles the onCreate and onDestroy events for the pool, then start and use the pool.

```
public void setupController() {

    // Initial max pool size
    int maxPoolSize = 10;

    // Create the object pool
    ObjectPooler<Object> pooler = new ObjectPooler<Object>( maxPoolSize, pooledObjectController );

    try {

        // Acquire an object lock
        Object object = pooler.get();

        // Do something with the object
        object.hashCode();

        // Release the object back to the pool
        pooler.release( object );
    }
    catch ( PooledObjectException e ) {
        System.out.println( "Failed to get an object from the pool." );
    }

    try {

        // Acquire an object lock
        Object object = pooler.get();

        // Destroy a single object
        pooler.destroy( object );
    }
    catch ( Exception e ) {
        System.out.println( "Failed to get an object from the pool." );
    }

    // Destroy all object in the pool
    pooler.shutdown();
}

// The object controller
private static PooledObjectController<MyObject> pooledObjectController = new PooledObjectController<MyObject>() {

    @Override
    public MyObject onCreate() {

        // On creation of a new object
        Object object = new Object();
        object.setSomething();

        // Return the newly created object
        return object;
    }

    @Override
    public void onDestroy( Object object ) {

        // On destruction of an object
        object.close();
        object.destroy();
    }
};
```

# License

Open source & free for all. ❤
