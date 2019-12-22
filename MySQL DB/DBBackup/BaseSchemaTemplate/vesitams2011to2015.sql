-- phpMyAdmin SQL Dump
-- version 4.0.4.1
-- http://www.phpmyadmin.net
--
-- Host: Localhost
-- Generation Time: Nov 10, 2013 at 03:24 AM
-- Server version: 5.6.11
-- PHP Version: 5.5.3

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `vesitams2011to2015`
--
CREATE DATABASE IF NOT EXISTS `vesitams2011to2015` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `vesitams2011to2015`;

-- --------------------------------------------------------

--
-- Table structure for table `academicyear`
--
-- Creation: Nov 06, 2013 at 11:41 PM
--

CREATE TABLE IF NOT EXISTS `academicyear` (
  `BatchID` varchar(256) NOT NULL,
  `StartDate` datetime NOT NULL,
  `EndDate` datetime NOT NULL,
  PRIMARY KEY (`BatchID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `branch`
--
-- Creation: Nov 06, 2013 at 11:47 PM
--

CREATE TABLE IF NOT EXISTS `branch` (
  `BranchID` int(11) NOT NULL AUTO_INCREMENT,
  `BranchName` varchar(1024) NOT NULL,
  `ShortName` varchar(256) DEFAULT NULL,
  `FloorNum` int(11) DEFAULT NULL,
  PRIMARY KEY (`BranchID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `divisions`
--
-- Creation: Nov 06, 2013 at 10:17 PM
--

CREATE TABLE IF NOT EXISTS `divisions` (
  `BranchID` int(11) NOT NULL,
  `StdID` int(11) NOT NULL,
  `DivID` int(11) NOT NULL AUTO_INCREMENT,
  `DivName` varchar(256) NOT NULL,
  `numStudents` int(11) DEFAULT NULL,
  PRIMARY KEY (`DivID`,`StdID`,`BranchID`),
  UNIQUE KEY `DivName` (`DivName`),
  KEY `BranchID` (`BranchID`),
  KEY `fk_stdid` (`StdID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- RELATIONS FOR TABLE `divisions`:
--   `BranchID`
--       `branch` -> `BranchID`
--   `StdID`
--       `standardyearname` -> `StdID`
--

-- --------------------------------------------------------

--
-- Table structure for table `registeredteachers`
--
-- Creation: Nov 10, 2013 at 12:02 AM
--

CREATE TABLE IF NOT EXISTS `registeredteachers` (
  `UsernamePWID` int(11) NOT NULL AUTO_INCREMENT,
  `TeacherName` varchar(512) NOT NULL,
  `Username` varchar(1024) NOT NULL,
  `Password` varchar(32768) NOT NULL,
  PRIMARY KEY (`UsernamePWID`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;

--
-- Dumping data for table `registeredteachers`
--

INSERT INTO `registeredteachers` (`UsernamePWID`, `TeacherName`, `Username`, `Password`) VALUES
(1, 'Ritesh', 'TCHR_1', 'password');

-- --------------------------------------------------------

--
-- Table structure for table `standardyearname`
--
-- Creation: Nov 06, 2013 at 11:48 PM
--

CREATE TABLE IF NOT EXISTS `standardyearname` (
  `StdID` int(11) NOT NULL AUTO_INCREMENT,
  `StdYearName` varchar(256) NOT NULL,
  `numDivisions` int(11) NOT NULL,
  `FloorNum` int(11) DEFAULT NULL,
  PRIMARY KEY (`StdID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `subjects`
--
-- Creation: Nov 10, 2013 at 02:24 AM
--

CREATE TABLE IF NOT EXISTS `subjects` (
  `BatchID` varchar(256) NOT NULL,
  `BranchID` int(11) NOT NULL,
  `StdID` int(11) NOT NULL,
  `DivID` int(11) NOT NULL,
  `SubjectID` int(11) NOT NULL,
  `LectureType` enum('ClassLecture','Practicals','Tutorials','IV','UnitTest','Prelims','Boards','MainExam') NOT NULL,
  `SubjectName` varchar(256) NOT NULL,
  `SubjectCode` varchar(32) DEFAULT NULL,
  `DurationHours` float NOT NULL,
  PRIMARY KEY (`BatchID`,`BranchID`,`StdID`,`DivID`,`SubjectID`,`LectureType`),
  UNIQUE KEY `SubjectName` (`SubjectName`),
  UNIQUE KEY `SubjectCode` (`SubjectCode`),
  KEY `BranchID` (`BranchID`),
  KEY `StdID` (`StdID`),
  KEY `DivID` (`DivID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- RELATIONS FOR TABLE `subjects`:
--   `BranchID`
--       `branch` -> `BranchID`
--   `StdID`
--       `standardyearname` -> `StdID`
--   `DivID`
--       `divisions` -> `DivID`
--   `BatchID`
--       `academicyear` -> `BatchID`
--

--
-- Constraints for dumped tables
--

--
-- Constraints for table `divisions`
--
ALTER TABLE `divisions`
  ADD CONSTRAINT `divisions_ibfk_1` FOREIGN KEY (`BranchID`) REFERENCES `branch` (`BranchID`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_stdid` FOREIGN KEY (`StdID`) REFERENCES `standardyearname` (`StdID`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `subjects`
--
ALTER TABLE `subjects`
  ADD CONSTRAINT `subjects_ibfk_1` FOREIGN KEY (`BranchID`) REFERENCES `branch` (`BranchID`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `subjects_ibfk_2` FOREIGN KEY (`StdID`) REFERENCES `standardyearname` (`StdID`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `subjects_ibfk_3` FOREIGN KEY (`DivID`) REFERENCES `divisions` (`DivID`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `subjects_ibfk_4` FOREIGN KEY (`BatchID`) REFERENCES `academicyear` (`BatchID`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
