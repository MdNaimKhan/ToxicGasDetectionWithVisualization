package com.thesisproject.gasmonitor.modeldata;

import com.thesisproject.gasmonitor.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CoordinatesData {
    List<Coordinate> data;

    public CoordinatesData() {
        setData();
    }

    public List<Coordinate> getData() {
        return data;
    }

    public void setData(List<Coordinate> data) {
        this.data = data;
    }

    private void setData() {
        data = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            Coordinate coordinate = new Coordinate();
            coordinate.setX(i);
            coordinate.setY(random.nextInt(10000));
            data.add(coordinate);
        }
    }
}
