package de.dlr.ivf.tapas.execution.sequential.guard;

import java.util.function.IntPredicate;

public class Guard implements IntPredicate {

    private int value_to_check_against;
    public Guard(int value_to_check_against){
        this.value_to_check_against = value_to_check_against;
    }

    @Override
    public boolean test(int i) {
        return i == this.value_to_check_against;
    }

    public void setValueToTest(int value){
        this.value_to_check_against = value;
    }

    public int getValueToTest(){
        return this.value_to_check_against;
    }
}
