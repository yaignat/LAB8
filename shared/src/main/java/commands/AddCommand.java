package commands;

import data.LabWork;

import java.io.Serial;
import java.io.Serializable;

public class AddCommand extends Command implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private LabWork labWork;

    public AddCommand(LabWork labWork) {
        super("add");
        this.labWork = labWork;
    }

    public LabWork getLabWork() {
        return labWork;
    }

    @Override
    public String execute(Object context) { return ""; }
}