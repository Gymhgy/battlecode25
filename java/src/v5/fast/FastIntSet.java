package v5.fast;

public class FastIntSet {
    public StringBuilder keys;
    public int size;

    public FastIntSet() {
        keys = new StringBuilder();
        size = 0;
    }

    public static int c2i(char a, char b) {
        return ((a & 0xFF) << 8) | (b & 0xFF);
    }
    public static String i2c(int value) {
        return (char) ((value >> 8) & 0xFF) + "" // Higher 8 bits
             + (char) (value & 0xFF);        // Lower 8 bits
    }

    public int size() {
        return size;
    }

    public void add(int i) {
        String key = i2c(i);
        if (keys.indexOf(key) < 0) {
            keys.append(key);
            size++;
        }
    }

    public void remove(int i) {
        String key = i2c(i);
        int index;
        if ((index = keys.indexOf(key)) >= 0) {
            keys.delete(index, index + 2);
            size--;
        }
    }

    public boolean contains(int i) {
        return keys.indexOf(i2c(i)) >= 0;
    }

    public void clear() {
        size = 0;
        keys = new StringBuilder();
    }

    public int pop() {
        if (size == 0)
            return -1;
        int i = peek();
        remove(i);
        return i;
    }
    public int peek() {
        if (size == 0)
            return -1;
        return c2i(keys.charAt(0), keys.charAt(1));
    }
}
