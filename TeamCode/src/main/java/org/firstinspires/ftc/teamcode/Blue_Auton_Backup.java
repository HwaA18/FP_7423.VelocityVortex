package org.firstinspires.ftc.teamcode;

import com.qualcomm.ftccommon.DbgLog;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cGyro;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.OpticalDistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;
import com.vuforia.HINT;
import com.vuforia.Vuforia;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import FlamingPhoenix.Direction;
import FlamingPhoenix.MecanumDriveTrain;
import FlamingPhoenix.MyUtility;
import FlamingPhoenix.TurnDirection;

/**
 * Created by HwaA1 on 11/12/2016.
 */

@Autonomous(name = "Blue_Auton_Old", group = "Autonomous")
@Disabled
public class Blue_Auton_Backup extends LinearOpMode {

    private VuforiaLocalizer vuforia;
    VuforiaTrackables tracker;

    DcMotor shooter;
    DcMotor collecter;

    Servo stopper;
    Servo pusher;

    ColorSensor color;

    OpticalDistanceSensor opt;

    @Override
    public void runOpMode() throws InterruptedException {
        shooter = hardwareMap.dcMotor.get("farriswheel");
        stopper = hardwareMap.servo.get("stopper");

        collecter = hardwareMap.dcMotor.get("collector");

        pusher = hardwareMap.servo.get("pusher");

        pusher.setPosition(.5);
        stopper.setPosition(.75);

        color = hardwareMap.colorSensor.get("color");
        color.enableLed(false);

        VuforiaLocalizer.Parameters parms = new VuforiaLocalizer.Parameters(R.id.cameraMonitorViewId);
        parms.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        parms.vuforiaLicenseKey = "AbYPrgD/////AAAAGbvKMH3NcEVFmPLgunQe4K0d1ZQi+afRLxricyooCq+sgY9Yh1j+bBrd0CdDCcoieA6trLCKBzymC515+Ps/FECtXv3+CTW6fg3/3+nvKZ6QA18h/cNZHg5HYHmghlcCgVUmSzOLRvdOpbS4S+0Y/sWGXwFK0PbuGPSN82w8XPDBoRYSWjAf8GXeitmNSlm9n4swrMoYNpMDuWCDjSm1kWnoErjFA9NuNoFzAgO+C/rYzoYjTJRk40ETVcAsahzatRlP7PJCvNNXiBhE6iVR+x7lFlTZ841xifOIOPkfVc54olC5XYe4A5ZmQ6WFD03W5HHdQrnmKPmkgcr1yqXAJ3rLTK8FZK3KVgbxz3Eeqps0";
        parms.cameraMonitorFeedback = VuforiaLocalizer.Parameters.CameraMonitorFeedback.AXES;

        this.vuforia = ClassFactory.createVuforiaLocalizer(parms);

        tracker = vuforia.loadTrackablesFromAsset("FTC_2016-17");

        Vuforia.setHint(HINT.HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 4);

        tracker.activate();

        ModernRoboticsI2cGyro gyro = (ModernRoboticsI2cGyro) hardwareMap.gyroSensor.get("gyro");

        gyro.resetZAxisIntegrator();

        opt = hardwareMap.opticalDistanceSensor.get("opt");

        while (gyro.isCalibrating() && this.opModeIsActive())
            Thread.sleep(50);

        MecanumDriveTrain wheels = new MecanumDriveTrain("frontleft", "frontright", "backleft", "backright", this);

        shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.idle();
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooter.setMaxSpeed(2550);

        gyro.resetZAxisIntegrator();
        gyro.calibrate();

        while(gyro.isCalibrating()) {
            Thread.sleep(50);
        }

        waitForStart();

        shooter.setPower(1);

        wheels.strafe(6, 0.7, TurnDirection.LEFT, this);
        Thread.sleep(1000);

        stopper.setPosition(.20);
        Thread.sleep(250);
        stopper.setPosition(.75);
        Thread.sleep(600);
        stopper.setPosition(0.20);
        Thread.sleep(500);
        collecter.setPower(.5);
        stopper.setPosition(.75);

        wheels.strafe(14, 0.5, TurnDirection.LEFT, this);

        collecter.setPower(0);
        shooter.setPower(0);

        wheels.drive(20, Direction.FORWARD, 0.4, 5, this);
        this.sleep(200);

        int angleBefore= gyro.getIntegratedZValue();
        int degreesNeeded = Math.abs(-88 - angleBefore);
        wheels.turnWithGyro(degreesNeeded, .2, TurnDirection.RIGHT, gyro, this);
        int angleAfter = gyro.getIntegratedZValue();

        DbgLog.msg("[Phoenix] angleBefore= %d, degreeNeeded= %d, angleAfter= %d", angleBefore, degreesNeeded, angleAfter);

        wheels.drive(20, Direction.BACKWARD, 0.3, 5, this);

        wheels.driveUntilImage(15, .1, Direction.BACKWARD, tracker.get(0), this);

        int angle = MyUtility.getImageAngle(tracker.get(0));
        DbgLog.msg("[Phoenix] angle2: " + angle);

        if (angle != -999) {//saw image and found the angle of the image
            int turnAngle = Math.abs(90 - angle);

            if(angle < 85) {
                DbgLog.msg("[Phoenix] Adjust to turn RIGHT by %d degree based on image angle of %d", turnAngle, angle);
                wheels.turnWithGyro(turnAngle, .5, TurnDirection.RIGHT, gyro, this);
            }
            else if(angle > 95) {
                DbgLog.msg("[Phoenix] Adjust to turn left by %d degree based on image angle of %d", turnAngle, angle);
                wheels.turnWithGyro(turnAngle, .5, TurnDirection.LEFT, gyro, this);
            }
        }

        int heading = gyro.getIntegratedZValue();
        wheels.resetMotorSpeed();
        double lastX = wheels.strafe(180, 0.5, TurnDirection.LEFT, tracker.get(0), heading, this);
        float imageX;

        int endHeading = gyro.getIntegratedZValue();
        int turningAngle = heading - endHeading;
        TurnDirection d;

        float adjustmentDistance;
        Direction adjustDirection;

        int didWeGoBack = 0;

        if(lastX != -9999) {
            endHeading = gyro.getIntegratedZValue();
            turningAngle = heading - endHeading;

            if (turningAngle < 0)
                d = TurnDirection.RIGHT;
            else
                d = TurnDirection.LEFT;

            if (d == TurnDirection.LEFT)
                DbgLog.msg("[Phoenix] At beacon, Reached beacon turn LEFT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
            else
                DbgLog.msg("[Phoenix] At beacon, Reached beacon turn RIGHT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);

            if (Math.abs(turningAngle) > 2) {
                wheels.turnWithGyro(Math.abs(turningAngle), .3, d, gyro, this);
                DbgLog.msg("[Phoenix] At beacon, performed Reached beacon turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
            }

            imageX = MyUtility.getImageXPosition(tracker.get(0));
            DbgLog.msg("[Phoenix] imageX=%9.3f", imageX);
            if ((imageX == -9999) && (lastX != -9999)) { //can't see image and we got the X during strafing
                imageX = (float) lastX;
                DbgLog.msg("[Phoenix] Can't see image, use last known imageX=%9.3f", imageX);
            } else if (imageX == -9999) {
                imageX = 0;
            }

            if (imageX < 0) {// We need to move back (to the left of image a bit)
                adjustDirection = Direction.FORWARD;
            } else if (imageX > 0) {
                adjustDirection = Direction.BACKWARD;
            } else {
                adjustDirection = Direction.FORWARD;
            }

            adjustmentDistance = Math.abs(((imageX * 100.0f)) / 254.0f) / 10.0f;
            if (adjustDirection == Direction.FORWARD)
                adjustmentDistance = adjustmentDistance + 1; //need to move forward a bit more to handle the strafing problem

            if (adjustDirection == Direction.BACKWARD)
                DbgLog.msg("[Phoenix] Beacon X position adjustment backward %7.3f and image X %7.3f", adjustmentDistance, imageX);
            else
                DbgLog.msg("[Phoenix] Beacon X position adjustment forward %7.3f and imageX %7.3f", adjustmentDistance, imageX);

            if (adjustmentDistance >= 1) {
                DbgLog.msg("[Phoenix] Performed Beacon X position adjustment %7.3f", adjustmentDistance);
                wheels.drive((int) adjustmentDistance, adjustDirection, 0.3, 2, this);
            }

            if (color.blue() <= 1) {
                DbgLog.msg("[Phoenix] Can't see blue, move back 5 inches");
                wheels.drive(8, Direction.BACKWARD, 0.3, 5, this);
                didWeGoBack = 5;
            }

            if (color.blue() > 1) { //sees the blue side
                wheels.strafe(3, .6, TurnDirection.LEFT, this);
                Thread.sleep(500);
                wheels.strafe(12, .5, TurnDirection.RIGHT, this);
            } else {
                wheels.strafe(10, .5, TurnDirection.RIGHT, this);
            }

            //First beacon (Gears) is complete
        }

        //Now go for 2nc Beacon, Adjust the angle first
        endHeading = gyro.getIntegratedZValue();
        turningAngle = heading - endHeading;

        if(turningAngle < 0)
            d = TurnDirection.RIGHT;
        else
            d = TurnDirection.LEFT;

        if (d == TurnDirection.LEFT)
            DbgLog.msg("[Phoenix] Leaving first beacon, Reached beacon turn LEFT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
        else
            DbgLog.msg("[Phoenix] Leaving first beacon, Reached beacon turn RIGHT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);

        if (Math.abs(turningAngle) > 5){
            wheels.turnWithGyro(Math.abs(turningAngle), .2, d, gyro, this);
            DbgLog.msg("[Phoenix] Leaving first beacon, performed Reached beacon turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
        }

        wheels.drive(54 - didWeGoBack, Direction.BACKWARD, 0.4, 4, this);

        wheels.driveUntilImage(15, 0.1, Direction.BACKWARD, tracker.get(2), this);

        angle = MyUtility.getImageAngle(tracker.get(2));
        DbgLog.msg("[Phoenix] 2nd beeacon angle: " + angle);

        if (angle != -999) {//saw image and found the angle of the image
            int turnAngle = Math.abs(90 - angle);

            if(angle < 85) {
                DbgLog.msg("[Phoenix] Adjust to turn RIGHT by %d degree based on 2nd image angle of %d", turnAngle, angle);
                wheels.turnWithGyro(turnAngle, .5, TurnDirection.RIGHT, gyro, this);
            }
            else if(angle > 95) {
                DbgLog.msg("[Phoenix] Adjust to turn left by %d degree based on 2nd image angle of %d", turnAngle, angle);
                wheels.turnWithGyro(turnAngle, .5, TurnDirection.LEFT, gyro, this);
            }
        }

        heading = gyro.getIntegratedZValue();
        wheels.resetMotorSpeed();
        lastX = wheels.strafe(180, 0.5, TurnDirection.LEFT, tracker.get(2), heading, this);

        endHeading = gyro.getIntegratedZValue();
        turningAngle = heading - endHeading;

        if(turningAngle < 0)
            d = TurnDirection.RIGHT;
        else
            d = TurnDirection.LEFT;

        if (d == TurnDirection.LEFT)
            DbgLog.msg("[Phoenix] At 2nd beacon, Reached beacon turn LEFT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
        else
            DbgLog.msg("[Phoenix] At 2nd beacon, Reached beacon turn RIGHT, turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);

        if (Math.abs(turningAngle) > 5){
            wheels.turnWithGyro(Math.abs(turningAngle), .2, d, gyro, this);
            DbgLog.msg("[Phoenix] At beacon, performed Reached beacon turningAngle= %d, heading= %d endHeading=%d", turningAngle, heading, endHeading);
        }

        imageX = MyUtility.getImageXPosition(tracker.get(2));
        DbgLog.msg("[Phoenix] 2nd imageX=%9.3f", imageX);
        if ((imageX == -9999) && (lastX != -9999)) { //can't see image and we got the X during strafing
            imageX = (float) lastX;
            DbgLog.msg("[Phoenix] Can't see 2nd image, use last known imageX=%9.3f", imageX);
        }
        else if (imageX == -9999) {
            imageX = 0;
        }

        if (imageX < 0) {// We need to move back (to the left of image a bit)
            adjustDirection = Direction.FORWARD;
        }
        else if (imageX > 0) {
            adjustDirection = Direction.BACKWARD;
        }
        else {
            adjustDirection = Direction.FORWARD;
        }

        adjustmentDistance = Math.abs(( (imageX * 100.0f))/ 254.0f) / 10.0f ;
        if (adjustDirection == Direction.FORWARD)
            adjustmentDistance = adjustmentDistance + 1; //need to move forward a bit more to handle the strafing problem

        if (adjustDirection == Direction.BACKWARD)
            DbgLog.msg("[Phoenix] 2nd Beacon X position adjustment backward %7.3f and image X %7.3f", adjustmentDistance, imageX);
        else
            DbgLog.msg("[Phoenix] 2nd Beacon X position adjustment forward %7.3f and imageX %7.3f", adjustmentDistance, imageX);

        if(adjustmentDistance >= 1) {
            DbgLog.msg("[Phoenix] Performed 2nd Beacon X position adjustment %7.3f", adjustmentDistance);
            wheels.drive((int) adjustmentDistance, adjustDirection, 0.3, 2, this);
        }

        if(color.blue() <= 1) {
            DbgLog.msg("[Phoenix] Can't see blue, move back 5 inches");
            wheels.drive(6, Direction.BACKWARD, 0.3, 5, this);
        }

        if (color.blue() > 1) { //sees the blue side
            wheels.strafe(5, .5, 2, TurnDirection.LEFT, this);
            Thread.sleep(500);
            wheels.strafe(8, .5, TurnDirection.RIGHT, this);
        }
        else {
            wheels.strafe(8, .5, TurnDirection.RIGHT, this);
        }

        wheels.turnWithGyro(35, .5, TurnDirection.RIGHT, gyro, this);
        collecter.setPower(-1);
        wheels.drive(52, Direction.FORWARD, .5, 6, this);
    }
}

