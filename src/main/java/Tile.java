import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class Tile {
    private boolean merged;
    private int value;

    public Tile(int value) {
        this.value = value;
    }

    void setMerged(boolean merged) {
        this.merged = merged;
    }

    boolean canMergeWith(Tile other) {
        return !merged && other != null && !other.merged && value == other.getValue();
    }

    int mergeWith(Tile other) {
        if (canMergeWith(other)) {
            value *= 2;
            merged = true;
            return value;
        }
        return -1;
    }
}
