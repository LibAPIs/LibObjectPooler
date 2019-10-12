```
import org.aihac.utils.pooler.ObjectPooler;
import org.aihac.utils.pooler.PooledObjectController;
import org.aihac.utils.pooler.PooledObjectException;

public class PoolerExample
{
    public static void main( String[] args )
    {
        // Initial max pool size
        int maxPoolSize = 10;

        // Create the object pool
        ObjectPooler<MyObject> pooler = new ObjectPooler<MyObject>( maxPoolSize, pooledObjectController );

        try
        {
            // Acquire an object lock
            MyObject myObject = pooler.get();

            // Do something with the object
            myObject.doSomething();

            // Release the object back to the pool
            pooler.release( myObject );
        }
        catch ( PooledObjectException e )
        {
            System.out.println( "Failed to get an object from the pool." );
        }

        try
        {
            // Acquire an object lock
            MyObject myObject = pooler.get();

            // Destroy a single object
            pooler.destroy( myObject );
        }
        catch ( Exception e )
        {
            System.out.println( "Failed to get an object from the pool." );
        }

        // Destroy all object in the pool
        pooler.shutdown();
    }

    // The object controller
    private static PooledObjectController<MyObject> pooledObjectController = new PooledObjectController<MyObject>()
    {
        @Override
        public MyObject onCreate()
        {
            // On creation of a new object
            MyObject myObject = new MyObject();
            myObject.setSomething();

            // Return the newly created object
            return myObject;
        }

        @Override
        public void onDestroy( MyObject myObject )
        {
            // On destruction of an object
            myObject.close();
            myObject.destroy();
        }
    };
}
```
