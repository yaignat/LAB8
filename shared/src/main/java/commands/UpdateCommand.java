package commands;

import data.LabWork;
import java.io.Serial;
import java.io.Serializable;

public class UpdateCommand extends Command implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private LabWork labWork;

    public UpdateCommand(Long id, LabWork labWork) {
        super("update");
        this.id = id;
        this.labWork = labWork;
    }

    public Long getId() {
        return id;
    }

    public LabWork getLabWork() {
        return labWork;
    }

    @Override
    public String execute(Object context) {
        return "";
    }
}