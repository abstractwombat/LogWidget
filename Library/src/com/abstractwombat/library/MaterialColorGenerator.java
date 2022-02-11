package com.abstractwombat.library;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Mike on 4/9/2015.
 */
public class MaterialColorGenerator {
    private static List<Integer> material = Arrays.asList(
            0xffe57373,
            0xfff06292,
            0xffba68c8,
            0xff9575cd,
            0xff7986cb,
            0xff64b5f6,
            0xff4fc3f7,
            0xff4dd0e1,
            0xff4db6ac,
            0xff81c784,
            0xffaed581,
            0xffff8a65,
            0xffd4e157,
            0xffffd54f,
            0xffffb74d,
            0xffa1887f,
            0xff90a4ae
    );

    public static int get(Object key) {
        return material.get(Math.abs(key.hashCode()) % material.size());
    }
}
