package org.firstinspires.ftc.teamcode.commandbase.instantCommands;

import com.arcrobotics.ftclib.command.InstantCommand;

import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Outtake;

public class StopperCommand extends InstantCommand {

    public StopperCommand(Outtake outtake, Outtake.StopperState state){
        super(
                () -> outtake.update(state)
        );
    }
}
