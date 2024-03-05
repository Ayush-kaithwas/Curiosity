package org.firstinspires.ftc.teamcode.auto;

import android.util.Size;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Drone;
import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Hanger;
import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Intake;
import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Outtake;
import org.firstinspires.ftc.teamcode.commandbase.Subsystems.Slider;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.vision.Globals;
import org.firstinspires.ftc.teamcode.vision.Location;
import org.firstinspires.ftc.teamcode.vision.PropPipeline;
import org.firstinspires.ftc.vision.VisionPortal;

import java.util.List;

@Autonomous
public class autoRedFarOpenCV extends LinearOpMode {

    List<LynxModule> allHubs = null;
    private PropPipeline propPipeline;
    private VisionPortal portal;
    private Location randomization;

    SampleMecanumDrive drive;
    public static double
            lifter_posL = 0, lifter_posR = 0, error_lifter, error_diff, error_int, error_lifterR, error_diffR, error_intR, errorprev, errorprevR, output_lifter, output_lifterR, output_power, target, dropVal;

    Slider slider = null;
    Hanger hanger = null;
    ElapsedTime timer;
    Intake intake = null;
    public static double kp = 3, ki, kd = 0.4;
    Outtake outtake = null;
    Drone drone = null;
    Servo dropLeft, dropRight;

    boolean left_flag, center_flag = false;

    @Override
    public void runOpMode() throws InterruptedException {
        drive = new SampleMecanumDrive(hardwareMap, telemetry);
        dropLeft = hardwareMap.get(Servo.class, "dropLeft");
        dropRight = hardwareMap.get(Servo.class, "dropRight");
        slider = new Slider(hardwareMap, telemetry);
        hanger = new Hanger(hardwareMap, telemetry);
        intake = new Intake(hardwareMap, telemetry);
        outtake = new Outtake(hardwareMap, telemetry);
        drone = new Drone(hardwareMap, telemetry);
        allHubs = hardwareMap.getAll(LynxModule.class);
        Outtake.crab.setPosition(0.38);
        Outtake.stopper.setPosition(0.5);
        Pose2d startPose = new Pose2d(-24-14, -62, Math.toRadians(90)); // original
        drive.setPoseEstimate(startPose);
        Drone.initialPos();
        Hanger.setHangerServo(Hanger.down_pos1, Hanger.down_pos2);
        dropLeft.setPosition(0.9);
        dropRight.setPosition(1);

//        CameraInitialization cam = new CameraInitialization();

//        allHubs = hardwareMap.getAll(LynxModule.class);
//        for(LynxModule hub : allHubs) {
//            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
//        }
//        for (LynxModule hub : allHubs) {
//            hub.clearBulkCache();
//        }
//
        Globals.ALLIANCE = Location.RED;
        Globals.SIDE = Location.FAR;

        propPipeline = new PropPipeline();
        portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(1280, 720))
                .addProcessor(propPipeline)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                .enableLiveView(true)
                .setAutoStopLiveView(false)
                .build();

        TrajectorySequence center = drive.trajectorySequenceBuilder(startPose)
                .splineToLinearHeading(new Pose2d(-24-12, -33, Math.toRadians(90)),Math.toRadians(90))
//                .waitSeconds(5)
                .addTemporalMarker(()->{
                    dropLeft.setPosition(0.6);
                })
                .waitSeconds(0.4)
                .lineToLinearHeading(new Pose2d(-24-14, -45, Math.toRadians(90)))
                .splineToLinearHeading(new Pose2d(-56.2, -10, Math.toRadians(-180)), Math.toRadians(90))
                //stack pick
                .addTemporalMarker(()->Intake.SetIntakePosition(0.47))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.IntakeStart())
                .waitSeconds(1.5)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.25))
                .lineToLinearHeading(new Pose2d(50, -12, Math.toRadians(-180)))
                .UNSTABLE_addTemporalMarkerOffset(-0.1, ()->{
                    Outtake.crab.setPosition(0.68);
                    output_power = lifter_pid(kp, ki, kd, 35);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(35, output_power);
                })
                .addTemporalMarker(()->Intake.IntakeStop())
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.6))
                .addTemporalMarker(()->Outtake.crab.setPosition(0.68))
                .waitSeconds(0.1)
                .addTemporalMarker(()->Intake.IntakeReverse())
                .waitSeconds(5)
                .lineToLinearHeading(new Pose2d(55.5, -34, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.6))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.7))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.85))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.9))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.98))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(1)
                .addTemporalMarker(()->{
                    output_power = lifter_pid(kp, ki, kd, 150);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(150, output_power);
                })
                .waitSeconds(0.5)
                .addTemporalMarker(()->Intake.IntakeStop())
                .lineToLinearHeading(new Pose2d(55.5, -24.5, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.crab.setPosition(0.5))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(0.7)
                .lineToLinearHeading(new Pose2d(50, -24.5, Math.toRadians(-180)))
                .addTemporalMarker(()->{ output_power = lifter_pid(kp, ki, kd, 0);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                })
                .addTemporalMarker(()->slider.extendTo(0, output_power))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0))
                .waitSeconds(0.3)
                .lineToLinearHeading(new Pose2d(55, -9, Math.toRadians(-180)))
                .lineToLinearHeading(new Pose2d(60, -9, Math.toRadians(-180)))
                .addTemporalMarker(()->Intake.SetIntakePosition(0.2))
                .build();

        TrajectorySequence right= drive.trajectorySequenceBuilder(startPose)
              .splineToLinearHeading(new Pose2d(-24-8.5, -25, Math.toRadians(0)),Math.toRadians(90))
                .addTemporalMarker(()->{
                    dropLeft.setPosition(0.6);
                })
                .waitSeconds(0.4)
                .lineToLinearHeading(new Pose2d(-24-18, -30, Math.toRadians(0)))
                .splineToLinearHeading(new Pose2d(-56, -10, Math.toRadians(-180)), Math.toRadians(90))
                //stack pick
                .addTemporalMarker(()->Intake.SetIntakePosition(0.47))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.IntakeStart())
                .waitSeconds(1)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.25))
                .lineToLinearHeading(new Pose2d(50, -12, Math.toRadians(-180)))
                .addTemporalMarker(()->{
                    Outtake.crab.setPosition(0.68);
                    output_power = lifter_pid(kp, ki, kd, 35);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(35, output_power);
                })
                .addTemporalMarker(()->Intake.IntakeStop())
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.6))
                .addTemporalMarker(()->Outtake.crab.setPosition(0.68))
                .waitSeconds(0.1)
                .addTemporalMarker(()->Intake.IntakeReverse())
                .waitSeconds(5)
                .lineToLinearHeading(new Pose2d(56, -41, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.6))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.7))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.85))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.9))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.98))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(1)
                .addTemporalMarker(()->{
                    output_power = lifter_pid(kp, ki, kd, 150);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(150, output_power);
                })
                .waitSeconds(0.5)
                .addTemporalMarker(()->Intake.IntakeStop())
                .lineToLinearHeading(new Pose2d(56, -36, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.crab.setPosition(0.5))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(0.7)
                .lineToLinearHeading(new Pose2d(50,-33, Math.toRadians(-180)))
                .addTemporalMarker(()->{ output_power = lifter_pid(kp, ki, kd, 0);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                })
                .addTemporalMarker(()->slider.extendTo(0, output_power))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0))
                .waitSeconds(0.3)
                .lineToLinearHeading(new Pose2d(55, -9, Math.toRadians(-180)))
                .lineToLinearHeading(new Pose2d(60, -9, Math.toRadians(-180)))
                .addTemporalMarker(()->Intake.SetIntakePosition(0.2))
                .build();

        TrajectorySequence left = drive.trajectorySequenceBuilder(startPose)
                .splineToLinearHeading(new Pose2d(-24-11.7, -34, Math.toRadians(-180)),Math.toRadians(90))
                .addTemporalMarker(()->{
                    dropLeft.setPosition(0.6);
                })
                .waitSeconds(0.3)
                .lineToLinearHeading(new Pose2d(-24-8, -36, Math.toRadians(-180)))
                .lineToLinearHeading(new Pose2d(-24-8, -20, Math.toRadians(-180)))
                .splineToLinearHeading(new Pose2d(-56.3, -10, Math.toRadians(-180)), Math.toRadians(-180))
                //stack pick
                .addTemporalMarker(()->Intake.SetIntakePosition(0.47))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.IntakeStart())
                .waitSeconds(1)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.25))
                .waitSeconds(0.1)
                .lineToLinearHeading(new Pose2d(50, -12, Math.toRadians(-180)))
                .addTemporalMarker( ()->{
                    Outtake.crab.setPosition(0.68);
                    output_power = lifter_pid(kp, ki, kd, 35);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(35, output_power);
                })
                .addTemporalMarker(()->Intake.IntakeStop())
                .waitSeconds(0.2)
                .addTemporalMarker(()->Intake.SetIntakePosition(0.6))
                .addTemporalMarker(()->Outtake.crab.setPosition(0.68))
                .waitSeconds(0.1)
                .addTemporalMarker(()->Intake.IntakeReverse())
                .waitSeconds(5)
                .lineToLinearHeading(new Pose2d(55.5, -25.5, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.6))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.7))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.85))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.9))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.setOuttakeArm(0.98))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(1)
                .addTemporalMarker(()->{
                    output_power = lifter_pid(kp, ki, kd, 150);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                    slider.extendTo(150, output_power);
                })
                .waitSeconds(0.5)
                .addTemporalMarker(()->Intake.IntakeStop())
                .lineToLinearHeading(new Pose2d(55.5, -24, Math.toRadians(-180)))
                .lineToLinearHeading(new Pose2d(55.5, -36, Math.toRadians(-180)))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0.7))
                .waitSeconds(0.5)
                .addTemporalMarker(()->Outtake.crab.setPosition(0.5))
                .waitSeconds(0.2)
                .addTemporalMarker(()->Outtake.stopper.setPosition(0.25))
                .waitSeconds(0.7)
                .lineToLinearHeading(new Pose2d(50,-36, Math.toRadians(-180)))
                .addTemporalMarker(()->{ output_power = lifter_pid(kp, ki, kd, 0);
                    if (output_power > 0.9) {
                        output_power = 1;
                    } else if (output_power < 0.2) {
                        output_power = 0;
                    }
                })
                .addTemporalMarker(()->slider.extendTo(0, output_power))
                .addTemporalMarker(()->Outtake.setOuttakeArm(0))
                .addTemporalMarker(()->Outtake.outtakeWrist.setPosition(0))
                .waitSeconds(0.3)
                .lineToLinearHeading(new Pose2d(55, -9, Math.toRadians(-180)))
                .lineToLinearHeading(new Pose2d(60, -9, Math.toRadians(-180)))
                .addTemporalMarker(()->Intake.SetIntakePosition(0.2))
                .build();

        while (opModeInInit()) {
            telemetry.addLine("ready");
            telemetry.addData("position", propPipeline.getLocation());
            if(gamepad1.x ){
                left_flag = true;
                telemetry.addLine("x pressed");
            }
            else if(gamepad1.y ){
                center_flag = true;
                telemetry.addLine("y pressed");
            }
            else if(gamepad1.b){
//                drive.followTrajectorySequence(right);
                telemetry.addLine("b pressed");
            }
            telemetry.update();
        }

        randomization = propPipeline.getLocation();
        portal.close();

        switch (randomization) {
            case LEFT:
//                drive.followTrajectorySequence(left);
                left_flag = true;
                break;
            case RIGHT:
//                drive.followTrajectorySequence(right);
                break;
            case CENTER:
//                drive.followTrajectorySequence(center);
                center_flag = true;
        }

        waitForStart();

        telemetry.addData("Position", randomization);
        telemetry.update();

        if(left_flag){
            drive.followTrajectorySequence(left);
        }
        else if(center_flag){
            drive.followTrajectorySequence(center);
        }
        else{
            drive.followTrajectorySequence(right);
        }

        drive.update();


    }

    public double lifter_pid(double kp_lifter, double ki_lifter, double kd_lifter, int target)
    {
        lifter_posL = Slider.sliderRight.getCurrentPosition();
        lifter_posR = Slider.sliderLeft.getCurrentPosition();

        error_lifter = target - lifter_posL;
        error_diff = error_lifter - errorprev;
        error_int = error_lifter + errorprev;
        output_lifter = kp_lifter*error_lifter + kd_lifter*error_diff +ki_lifter*error_int;

        error_lifterR = target - lifter_posR;
        error_diffR = error_lifterR - errorprevR;
        error_intR = error_lifterR + errorprevR;
        output_lifterR = kp_lifter*error_lifterR + kd_lifter*error_diffR +ki_lifter*error_intR;

        errorprev = error_lifter;
        errorprevR = error_lifterR;
        return Math.abs(output_lifter);
    }
}
