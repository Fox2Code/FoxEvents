# FoxEvents

Just a simple event system

Support Java8+

### Example

```java
public class Example {
    public static final EventHolder<MyEvent> HOLDER = EventHolder.getHolderFromEvent(MyEvent.class);
    
    public static void myEvent(int property) {
        if (!HOLDER.isEmpty()) {
            new MyEvent(property).callEvent();
        }
    }
}
```
