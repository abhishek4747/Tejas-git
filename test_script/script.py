import subprocess
import math
import os

if __name__=="__main__":
	# Read config file
	tejasdir = "/home/abhishek/Tejas-git/"
	jarfile = "/home/abhishek/Desktop/test_tejas.jar"
	scriptdir = tejasdir+"test_script/"
	configfile = scriptdir+'config.xml'
	resultdir  = scriptdir +'results/'
	resultfile = resultdir+'result_%d_%d_%d_%d.txt'
	if not os.path.exists(resultdir):
		os.mkdir(resultdir)
	config_file = open(configfile).readlines()
	print "\n"
	testno = 4
	cdbsize = 10
	robsize = 100
	rssize = 10
	print "Test Num\tCDB Size\t ROB Size\t RS Size\tTotal cycles"
	for testno in range(8):
		for cdbsize in range(1,11,2):
			config_file[84] = "<CDBNumPorts>%d</CDBNumPorts>\n"%cdbsize
			for robsize in range(10,100,10):
				config_file[166] = "<ROBSize>%d</ROBSize>\n"%robsize
				for rssize in range(5,30,5):
					config_file[83] = "<NumReservationStations>%d</NumReservationStations>\n"%rssize
					open(configfile,'w').writelines(config_file)
					rfile = resultfile%(testno, cdbsize, robsize, rssize)
					cmd = ['java','-jar', jarfile, configfile, str(testno), rfile]
					cmd = map(str, cmd)
					out,err = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
					# print out
					# print err
					output = open(rfile).readlines()		
					tc = int(output[19].split('\n')[0].split('=')[1].strip())
					print "%d\t\t%d\t\t%d\t\t%d\t\t%d"%(testno,cdbsize,robsize,rssize,tc)
