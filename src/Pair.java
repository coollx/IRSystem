public class Pair<K, V extends Comparable<V>> implements Comparable<Pair<K, V>> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        return this.value = value;
    }

    @Override
    public String toString() {
        return "<" + key + " - " + value + ">";
    }

    @Override
    public int compareTo(Pair<K, V> other) {
        return other.value.compareTo(this.value);
    }
}